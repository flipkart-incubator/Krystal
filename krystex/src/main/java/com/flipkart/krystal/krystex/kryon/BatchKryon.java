package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.data.ValueOrError.empty;
import static com.flipkart.krystal.data.ValueOrError.withError;
import static com.flipkart.krystal.futures.Futures.linkFutures;
import static com.flipkart.krystal.krystex.kryon.KryonUtils.enqueueOrExecuteCommand;
import static com.flipkart.krystal.krystex.resolution.ResolverCommand.multiExecuteWith;
import static com.flipkart.krystal.krystex.resolution.ResolverCommand.skip;
import static com.google.common.base.Functions.identity;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.commands.BatchCommand;
import com.flipkart.krystal.krystex.commands.CallbackBatch;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardBatch;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import com.flipkart.krystal.krystex.resolution.DependencyResolutionRequest;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverCommand;
import com.flipkart.krystal.krystex.resolution.ResolverCommand.SkipDependency;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.flipkart.krystal.model.DependantChain;
import com.flipkart.krystal.model.KryonId;
import com.flipkart.krystal.utils.SkippedExecutionException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

final class BatchKryon extends AbstractKryon<BatchCommand, BatchResponse> {

  private final Map<DependantChain, Set<String>> availableInputsByDepChain = new LinkedHashMap<>();

  private final Map<DependantChain, ForwardBatch> inputsValueCollector = new LinkedHashMap<>();

  private final Map<DependantChain, Map<String, CallbackBatch>> dependencyValuesCollector =
      new LinkedHashMap<>();

  /** A unique Result future for every dependant chain. */
  private final Map<DependantChain, CompletableFuture<BatchResponse>> resultsByDepChain =
      new LinkedHashMap<>();

  /**
   * A unique {@link CompletableFuture} for every new set of Inputs. This acts as a cache so that
   * the same computation is not repeated multiple times .
   */
  private final Map<Inputs, CompletableFuture<@Nullable Object>> resultsCache =
      new LinkedHashMap<>();

  private final Map<DependantChain, Set<String>> executedDependencies = new LinkedHashMap<>();

  private final Map<DependantChain, Set<RequestId>> requestsByDependantChain =
      new LinkedHashMap<>();

  private final Set<DependantChain> flushedDependantChain = new LinkedHashSet<>();
  private final Map<DependantChain, Boolean> mainLogicExecuted = new LinkedHashMap<>();

  BatchKryon(
      KryonDefinition kryonDefinition,
      KryonExecutor kryonExecutor,
      Function<LogicExecutionContext, ImmutableMap<String, MainLogicDecorator>>
          requestScopedDecoratorsSupplier,
      LogicDecorationOrdering logicDecorationOrdering,
      RequestIdGenerator requestIdGenerator) {
    super(
        kryonDefinition,
        kryonExecutor,
        requestScopedDecoratorsSupplier,
        logicDecorationOrdering,
        requestIdGenerator);
  }

  @Override
  public void executeCommand(Flush flushCommand) {
    flushedDependantChain.add(flushCommand.dependantChain());
    flushAllDependenciesIfNeeded(flushCommand.dependantChain());
    flushDecoratorsIfNeeded(flushCommand.dependantChain());
  }

  @Override
  public CompletableFuture<BatchResponse> executeCommand(BatchCommand kryonCommand) {
    DependantChain dependantChain = kryonCommand.dependantChain();
    final CompletableFuture<BatchResponse> resultForDepChain =
        resultsByDepChain.computeIfAbsent(dependantChain, r -> new CompletableFuture<>());
    try {
      if (kryonCommand instanceof ForwardBatch forwardBatch) {
        collectInputValues(forwardBatch);
      } else if (kryonCommand instanceof CallbackBatch callbackBatch) {
        collectDependencyValues(callbackBatch);
      }
      triggerDependencies(
          dependantChain, getTriggerableDependencies(dependantChain, kryonCommand.inputNames()));

      Optional<CompletableFuture<BatchResponse>> mainLogicFuture =
          executeMainLogicIfPossible(dependantChain);
      mainLogicFuture.ifPresent(f -> linkFutures(f, resultForDepChain));
    } catch (Throwable e) {
      resultForDepChain.completeExceptionally(e);
    }
    return resultForDepChain;
  }

  private Map<String, Set<ResolverDefinition>> getTriggerableDependencies(
      DependantChain dependantChain, Set<String> newInputNames) {
    Set<String> availableInputs = availableInputsByDepChain.getOrDefault(dependantChain, Set.of());
    Set<String> executedDeps = executedDependencies.getOrDefault(dependantChain, Set.of());

    return Stream.concat(
            Stream.concat(
                    Stream.of(Optional.<String>empty()), newInputNames.stream().map(Optional::of))
                .map(key -> resolverDefinitionsByInput.getOrDefault(key, ImmutableSet.of()))
                .flatMap(Collection::stream)
                .map(ResolverDefinition::dependencyName),
            dependenciesWithNoResolvers.stream())
        .distinct()
        .filter(depName -> !executedDeps.contains(depName))
        .filter(
            depName ->
                resolverDefinitionsByDependencies.getOrDefault(depName, ImmutableSet.of()).stream()
                    .map(ResolverDefinition::boundFrom)
                    .flatMap(Collection::stream)
                    .allMatch(availableInputs::contains))
        .collect(
            toMap(
                identity(),
                depName ->
                    resolverDefinitionsByDependencies.getOrDefault(depName, ImmutableSet.of())));
  }

  private void triggerDependencies(
      DependantChain dependantChain, Map<String, Set<ResolverDefinition>> triggerableDependencies) {
    ForwardBatch forwardBatch = getForwardCommand(dependantChain);

    Optional<MultiResolverDefinition> multiResolverOpt =
        kryonDefinition
            .multiResolverLogicId()
            .map(
                kryonLogicId ->
                    kryonDefinition
                        .kryonDefinitionRegistry()
                        .logicDefinitionRegistry()
                        .getMultiResolver(kryonLogicId));
    ImmutableMap<RequestId, String> skippedRequests = forwardBatch.skippedRequests();
    ImmutableSet<RequestId> executableRequests = forwardBatch.executableRequests().keySet();
    Map<String, Map<Set<RequestId>, ResolverCommand>> commandsByDependency = new LinkedHashMap<>();
    if (!skippedRequests.isEmpty()) {
      SkipDependency skip = skip(String.join(", ", skippedRequests.values()));
      for (String depName : triggerableDependencies.keySet()) {
        commandsByDependency
            .computeIfAbsent(depName, _k -> new LinkedHashMap<>())
            .put(skippedRequests.keySet(), skip);
      }
    }

    Set<String> dependenciesWithNoResolvers =
        triggerableDependencies.entrySet().stream()
            .filter(e -> e.getValue().isEmpty())
            .map(Entry::getKey)
            .collect(toSet());
    for (RequestId requestId : executableRequests) {
      dependenciesWithNoResolvers.forEach(
          depName -> {
            // For such dependencies, trigger them with empty inputs
            commandsByDependency
                .computeIfAbsent(depName, _k -> new LinkedHashMap<>())
                .put(Set.of(requestId), multiExecuteWith(ImmutableList.of(Inputs.empty())));
          });
      Inputs inputs =
          getInputsFor(
              dependantChain,
              requestId,
              triggerableDependencies.values().stream()
                  .flatMap(Collection::stream)
                  .map(ResolverDefinition::boundFrom)
                  .flatMap(Collection::stream)
                  .collect(toSet()));
      multiResolverOpt
          .map(LogicDefinition::logic)
          .map(
              logic -> {
                return logic.resolve(
                    triggerableDependencies.entrySet().stream()
                        .filter(e -> !e.getValue().isEmpty())
                        .map(e -> new DependencyResolutionRequest(e.getKey(), e.getValue()))
                        .toList(),
                    inputs);
              })
          .orElse(ImmutableMap.of())
          .forEach(
              (depName, resolverCommand) -> {
                commandsByDependency
                    .computeIfAbsent(depName, _k -> new LinkedHashMap<>())
                    .put(Set.of(requestId), resolverCommand);
              });
    }
    for (var entry : commandsByDependency.entrySet()) {
      String depName = entry.getKey();
      var resolverCommandsForDep = entry.getValue();
      triggerDependency(
          depName,
          dependantChain,
          resolverCommandsForDep,
          triggerableDependencies.getOrDefault(depName, ImmutableSet.of()));
    }
  }

  private ForwardBatch getForwardCommand(DependantChain dependantChain) {
    ForwardBatch forwardBatch = inputsValueCollector.get(dependantChain);
    if (forwardBatch == null) {
      throw new IllegalArgumentException("Missing Forward command. This should not be possible.");
    }
    return forwardBatch;
  }

  private void triggerDependency(
      String depName,
      DependantChain dependantChain,
      Map<Set<RequestId>, ResolverCommand> resolverCommandsByReq,
      Set<ResolverDefinition> resolverDefinitions) {
    KryonId depKryonId = kryonDefinition.dependencyKryons().get(depName);
    if (depKryonId == null) {
      throw new AssertionError("This is a bug.");
    }
    Map<RequestId, Inputs> inputsByDepReq = new LinkedHashMap<>();
    Map<RequestId, String> skipReasonsByReq = new LinkedHashMap<>();
    Map<RequestId, Set<RequestId>> depReqsByIncomingReq = new LinkedHashMap<>();
    for (var entry : resolverCommandsByReq.entrySet()) {
      Set<RequestId> incomingReqIds = entry.getKey();
      ResolverCommand resolverCommand = entry.getValue();
      if (resolverCommand instanceof SkipDependency skipDependency) {
        RequestId depReqId =
            requestIdGenerator.newSubRequest(
                incomingReqIds.iterator().next(), () -> "%s[skip]".formatted(depName));
        incomingReqIds.forEach(
            incomingReqId ->
                depReqsByIncomingReq
                    .computeIfAbsent(incomingReqId, _k -> new LinkedHashSet<>())
                    .add(depReqId));
        skipReasonsByReq.put(depReqId, skipDependency.reason());
      } else {
        int count = 0;
        for (RequestId incomingReqId : incomingReqIds) {
          if (resolverCommand.getInputs().isEmpty()) {
            RequestId depReqId =
                requestIdGenerator.newSubRequest(
                    incomingReqId, () -> "%s[skip]".formatted(depName));
            skipReasonsByReq.put(
                depReqId, "Resolvers for dependency %s resolved to empty list".formatted(depName));
          } else {
            for (Inputs inputs : resolverCommand.getInputs()) {
              int currentCount = count++;
              RequestId depReqId =
                  requestIdGenerator.newSubRequest(
                      incomingReqId, () -> "%s[%s]".formatted(depName, currentCount));
              depReqsByIncomingReq
                  .computeIfAbsent(incomingReqId, _k -> new LinkedHashSet<>())
                  .add(depReqId);
              inputsByDepReq.put(depReqId, inputs);
            }
          }
        }
      }
    }
    executedDependencies.computeIfAbsent(dependantChain, _k -> new LinkedHashSet<>()).add(depName);
    CompletableFuture<BatchResponse> depResponse =
        kryonExecutor.executeCommand(
            new ForwardBatch(
                depKryonId,
                resolverDefinitions.stream()
                    .map(ResolverDefinition::resolvedInputNames)
                    .flatMap(Collection::stream)
                    .collect(toImmutableSet()),
                ImmutableMap.copyOf(inputsByDepReq),
                dependantChain.extend(kryonId, depName),
                ImmutableMap.copyOf(skipReasonsByReq)));

    depResponse.whenComplete(
        (batchResponse, throwable) -> {
          Set<RequestId> requestIds =
              resolverCommandsByReq.keySet().stream().flatMap(Collection::stream).collect(toSet());
          ImmutableMap<RequestId, Results<Object>> results =
              requestIds.stream()
                  .collect(
                      toImmutableMap(
                          identity(),
                          requestId -> {
                            if (throwable != null) {
                              return new Results<>(
                                  ImmutableMap.of(Inputs.empty(), withError(throwable)));
                            } else {
                              Set<RequestId> depReqIds =
                                  depReqsByIncomingReq.getOrDefault(requestId, Set.of());
                              return new Results<>(
                                  depReqIds.stream()
                                      .collect(
                                          toImmutableMap(
                                              depReqId ->
                                                  inputsByDepReq.getOrDefault(
                                                      depReqId, Inputs.empty()),
                                              depReqId ->
                                                  batchResponse
                                                      .responses()
                                                      .getOrDefault(depReqId, empty()))));
                            }
                          }));

          enqueueOrExecuteCommand(
              () -> new CallbackBatch(kryonId, depName, results, dependantChain),
              depKryonId,
              kryonDefinition,
              kryonExecutor);
        });
    flushDependencyIfNeeded(depName, dependantChain);
  }

  private Optional<CompletableFuture<BatchResponse>> executeMainLogicIfPossible(
      DependantChain dependantChain) {
    ForwardBatch forwardCommand = getForwardCommand(dependantChain);
    // If all the inputs and dependency values are available, then prepare run mainLogic
    ImmutableSet<String> inputNames = kryonDefinition.getMainLogicDefinition().inputNames();
    if (availableInputsByDepChain
        .getOrDefault(dependantChain, ImmutableSet.of())
        .containsAll(inputNames)) { // All the inputs of the kryon logic have data present
      if (forwardCommand.shouldSkip()) {
        return Optional.of(
            failedFuture(new SkippedExecutionException(getSkipMessage(forwardCommand))));
      }
      return Optional.of(
          executeMainLogic(forwardCommand.executableRequests().keySet(), dependantChain));
    }
    return Optional.empty();
  }

  private CompletableFuture<BatchResponse> executeMainLogic(
      Set<RequestId> requestIds, DependantChain dependantChain) {

    MainLogicDefinition<Object> mainLogicDefinition = kryonDefinition.getMainLogicDefinition();

    Map<RequestId, MainLogicInputs> mainLogicInputs = new LinkedHashMap<>();

    for (RequestId requestId : requestIds) {
      mainLogicInputs.put(requestId, getInputsForMainLogic(dependantChain, requestId));
    }
    CompletableFuture<BatchResponse> resultForBatch = new CompletableFuture<>();
    Map<RequestId, CompletableFuture<ValueOrError<Object>>> results =
        executeDecoratedMainLogic(mainLogicDefinition, mainLogicInputs, dependantChain);

    allOf(results.values().toArray(CompletableFuture[]::new))
        .whenComplete(
            (unused, throwable) -> {
              resultForBatch.complete(
                  new BatchResponse(
                      mainLogicInputs.keySet().stream()
                          .collect(
                              toImmutableMap(
                                  identity(),
                                  requestId ->
                                      results
                                          .getOrDefault(requestId, new CompletableFuture<>())
                                          .getNow(empty())))));
            });
    mainLogicExecuted.put(dependantChain, true);
    flushDecoratorsIfNeeded(dependantChain);
    return resultForBatch;
  }

  private Map<RequestId, CompletableFuture<ValueOrError<Object>>> executeDecoratedMainLogic(
      MainLogicDefinition<Object> mainLogicDefinition,
      Map<RequestId, MainLogicInputs> inputs,
      DependantChain dependantChain) {
    NavigableSet<MainLogicDecorator> sortedDecorators = getSortedDecorators(dependantChain);
    MainLogic<Object> logic = mainLogicDefinition::execute;

    for (MainLogicDecorator mainLogicDecorator : sortedDecorators) {
      logic = mainLogicDecorator.decorateLogic(logic, mainLogicDefinition);
    }
    MainLogic<Object> finalLogic = logic;
    Map<RequestId, CompletableFuture<ValueOrError<Object>>> resultsByRequest =
        new LinkedHashMap<>();
    inputs.forEach(
        (requestId, mainLogicInputs) -> {
          // Retrieve existing result from cache if result for this set of inputs has already been
          // calculated
          CompletableFuture<@Nullable Object> cachedResult =
              resultsCache.get(mainLogicInputs.providedInputs());
          if (cachedResult == null) {
            try {
              cachedResult =
                  finalLogic
                      .execute(ImmutableList.of(mainLogicInputs.allInputsAndDependencies()))
                      .values()
                      .iterator()
                      .next();
            } catch (Exception e) {
              cachedResult = failedFuture(e);
            }
            resultsCache.put(mainLogicInputs.providedInputs(), cachedResult);
          }
          resultsByRequest.put(requestId, cachedResult.handle(ValueOrError::valueOrError));
        });
    return resultsByRequest;
  }

  private void flushAllDependenciesIfNeeded(DependantChain dependantChain) {
    kryonDefinition
        .dependencyKryons()
        .keySet()
        .forEach(dependencyName -> flushDependencyIfNeeded(dependencyName, dependantChain));
  }

  private void flushDependencyIfNeeded(String dependencyName, DependantChain dependantChain) {
    if (!flushedDependantChain.contains(dependantChain)) {
      return;
    }
    if (executedDependencies.getOrDefault(dependantChain, Set.of()).contains(dependencyName)) {
      kryonExecutor.executeCommand(
          new Flush(
              Optional.ofNullable(kryonDefinition.dependencyKryons().get(dependencyName))
                  .orElseThrow(
                      () ->
                          new AssertionError(
                              "Could not find KryonId for dependency "
                                  + dependencyName
                                  + ". This is a bug")),
              dependantChain.extend(kryonId, dependencyName)));
    }
  }

  private void flushDecoratorsIfNeeded(DependantChain dependantChain) {
    if (!flushedDependantChain.contains(dependantChain)) {
      return;
    }
    if (mainLogicExecuted.getOrDefault(dependantChain, false)
        || getForwardCommand(dependantChain).shouldSkip()) {
      Iterable<MainLogicDecorator> reverseSortedDecorators =
          getSortedDecorators(dependantChain)::descendingIterator;
      for (MainLogicDecorator decorator : reverseSortedDecorators) {
        decorator.executeCommand(new FlushCommand(dependantChain));
      }
    }
  }

  private Inputs getInputsFor(
      DependantChain dependantChain, RequestId requestId, Set<String> boundFrom) {
    Inputs resolvableInputs =
        Optional.ofNullable(inputsValueCollector.get(dependantChain))
            .map(ForwardBatch::executableRequests)
            .map(inputsByRequest -> inputsByRequest.get(requestId))
            .orElse(Inputs.empty());
    Map<String, CallbackBatch> depValues =
        dependencyValuesCollector.getOrDefault(dependantChain, Map.of());
    Map<String, InputValue<Object>> inputValues = new LinkedHashMap<>();
    for (String boundFromInput : boundFrom) {
      InputValue<Object> voe = resolvableInputs.values().get(boundFromInput);
      if (voe == null) {
        CallbackBatch callbackBatch = depValues.get(boundFromInput);
        if (callbackBatch != null) {
          inputValues.put(
              boundFromInput,
              callbackBatch.resultsByRequest().getOrDefault(requestId, Results.empty()));
        }
      } else {
        inputValues.put(boundFromInput, voe);
      }
    }
    return new Inputs(inputValues);
  }

  private MainLogicInputs getInputsForMainLogic(
      DependantChain dependantChain, RequestId requestId) {
    ForwardBatch forwardBatch = inputsValueCollector.get(dependantChain);
    if (forwardBatch == null) {
      throw new AssertionError("Could not find forwardBatch. This is a bug.");
    }
    ImmutableMap<String, Results<Object>> depValues =
        dependencyValuesCollector
            .getOrDefault(dependantChain, ImmutableMap.of())
            .entrySet()
            .stream()
            .collect(
                toImmutableMap(
                    Entry::getKey,
                    e -> e.getValue().resultsByRequest().getOrDefault(requestId, Results.empty())));
    Inputs inputValues = forwardBatch.executableRequests().getOrDefault(requestId, Inputs.empty());
    Inputs allInputsAndDependencies = Inputs.union(depValues, inputValues.values());
    return new MainLogicInputs(inputValues, allInputsAndDependencies);
  }

  private void collectInputValues(ForwardBatch forwardBatch) {
    if (requestsByDependantChain.putIfAbsent(
            forwardBatch.dependantChain(), forwardBatch.requestIds())
        != null) {
      throw new DuplicateRequestException(
          "Duplicate batch request received for dependant chain %s"
              .formatted(forwardBatch.dependantChain()));
    }
    ImmutableSet<String> inputNames = forwardBatch.inputNames();
    if (inputsValueCollector.putIfAbsent(forwardBatch.dependantChain(), forwardBatch) != null) {
      throw new DuplicateRequestException(
          "Duplicate data for inputs %s of kryon %s in dependant chain %s"
              .formatted(inputNames, kryonId, forwardBatch.dependantChain()));
    }
    SetView<String> resolvableInputNames =
        Sets.difference(
            kryonDefinition.getMainLogicDefinition().inputNames(),
            kryonDefinition.dependencyKryons().keySet());
    if (!inputNames.containsAll(resolvableInputNames)) {
      throw new IllegalArgumentException(
          "Did not receive inputs " + Sets.difference(resolvableInputNames, inputNames));
    }
    availableInputsByDepChain
        .computeIfAbsent(forwardBatch.dependantChain(), _k -> new LinkedHashSet<>())
        .addAll(inputNames);
  }

  private static String getSkipMessage(ForwardBatch forwardBatch) {
    return String.join(", ", forwardBatch.skippedRequests().values());
  }

  private void collectDependencyValues(CallbackBatch callbackBatch) {
    String dependencyName = callbackBatch.dependencyName();
    availableInputsByDepChain
        .computeIfAbsent(callbackBatch.dependantChain(), _k -> new LinkedHashSet<>())
        .add(dependencyName);
    if (dependencyValuesCollector
            .computeIfAbsent(callbackBatch.dependantChain(), k -> new LinkedHashMap<>())
            .putIfAbsent(dependencyName, callbackBatch)
        != null) {
      throw new DuplicateRequestException(
          "Duplicate data for dependency %s of kryon %s in dependant chain %s"
              .formatted(dependencyName, kryonId, callbackBatch.dependantChain()));
    }
  }
}
