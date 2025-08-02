package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.flipkart.krystal.data.Errable.withError;
import static com.flipkart.krystal.except.StackTracelessException.stackTracelessWrap;
import static com.flipkart.krystal.krystex.kryon.KryonUtils.enqueueOrExecuteCommand;
import static com.flipkart.krystal.krystex.resolution.ResolverCommand.multiExecuteWith;
import static com.flipkart.krystal.krystex.resolution.ResolverCommand.skip;
import static com.google.common.base.Functions.identity;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.except.SkippedExecutionException;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.BatchCommand;
import com.flipkart.krystal.krystex.commands.CallbackBatch;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardBatch;
import com.flipkart.krystal.krystex.logicdecoration.FlushCommand;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import com.flipkart.krystal.krystex.resolution.DependencyResolutionRequest;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverCommand;
import com.flipkart.krystal.krystex.resolution.ResolverCommand.SkipDependency;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
final class BatchKryon extends AbstractKryon<BatchCommand, BatchResponse> {

  private final Map<DependantChain, Set<String>> availableInputsByDepChain = new LinkedHashMap<>();

  private final Map<DependantChain, ForwardBatch> inputsValueCollector = new LinkedHashMap<>();

  private final Map<DependantChain, Map<String, CallbackBatch>> dependencyValuesCollector =
      new LinkedHashMap<>();

  /** A unique Result future for every dependant chain. */
  private final Map<DependantChain, CompletableFuture<BatchResponse>> resultsByDepChain =
      new LinkedHashMap<>();

  private final Map<DependantChain, Set<String>> executedDependencies = new LinkedHashMap<>();

  private final Map<DependantChain, Set<RequestId>> requestsByDependantChain =
      new LinkedHashMap<>();

  private final Set<DependantChain> flushedDependantChain = new LinkedHashSet<>();
  private final Map<DependantChain, Boolean> outputLogicExecuted = new LinkedHashMap<>();

  BatchKryon(
      KryonDefinition kryonDefinition,
      KryonExecutor kryonExecutor,
      Function<LogicExecutionContext, ImmutableMap<String, OutputLogicDecorator>>
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
      Set<String> newFacetNames = ImmutableSet.of();
      if (kryonCommand instanceof ForwardBatch forwardBatch) {
        newFacetNames = kryonDefinition.givenFacets();
        if (log.isDebugEnabled()) {
          forwardBatch
              .executableRequests()
              .forEach(
                  (requestId, facets) -> {
                    log.debug(
                        "Exec Ids - {}: {} invoked with inputs {}, in call path {}",
                        requestId,
                        kryonId,
                        facets,
                        forwardBatch.dependantChain());
                  });
        }
        collectInputValues(forwardBatch);
      } else if (kryonCommand instanceof CallbackBatch callbackBatch) {
        newFacetNames = Set.of(callbackBatch.dependencyName());
        if (log.isDebugEnabled()) {
          callbackBatch
              .resultsByRequest()
              .forEach(
                  (requestId, results) -> {
                    log.debug(
                        "Exec Ids - {}: {} received response for dependency {} in call path {}. Response: {}",
                        requestId,
                        kryonId,
                        callbackBatch.dependencyName(),
                        callbackBatch.dependantChain(),
                        results);
                  });
        }
        collectDependencyValues(callbackBatch);
      }
      triggerDependencies(
          dependantChain, getTriggerableDependencies(dependantChain, newFacetNames));

      Optional<CompletableFuture<BatchResponse>> outputLogicFuture =
          executeOutputLogicIfPossible(dependantChain);
      outputLogicFuture.ifPresent(f -> linkFutures(f, resultForDepChain));
    } catch (Throwable e) {
      resultForDepChain.completeExceptionally(stackTracelessWrap(e));
    }
    return resultForDepChain;
  }

  private Map<String, Set<ResolverDefinition>> getTriggerableDependencies(
      DependantChain dependantChain, Set<String> newFacetNames) {
    Set<String> availableInputs = availableInputsByDepChain.getOrDefault(dependantChain, Set.of());
    Set<String> executedDeps = executedDependencies.getOrDefault(dependantChain, Set.of());
    Set<String> dependenciesForNewFacetNames = new LinkedHashSet<>();
    for (String newFacetName : newFacetNames) {
      ImmutableMap<String, ImmutableSet<String>> dependenciesByBoundFacet =
          kryonDefinition.dependenciesByBoundFacet();
      dependenciesForNewFacetNames.addAll(
          dependenciesByBoundFacet.getOrDefault(newFacetName, ImmutableSet.of()));
    }
    Map<String, Set<ResolverDefinition>> triggerableDependencies = new LinkedHashMap<>();
    for (String depName :
        Iterables.concat(
            dependenciesForNewFacetNames,
            kryonDefinition.dependenciesWithNoFacetResolvers(),
            kryonDefinition.dependenciesWithNoResolvers())) {
      if (executedDeps.contains(depName)) {
        continue;
      }
      if (availableInputs.containsAll(
          kryonDefinition
              .dependencyToBoundFacetsMapping()
              .getOrDefault(depName, ImmutableSet.of()))) {
        triggerableDependencies.put(
            depName,
            kryonDefinition
                .resolverDefinitionsByDependencies()
                .getOrDefault(depName, ImmutableSet.of()));
      }
    }
    return triggerableDependencies;
  }

  private void triggerDependencies(
      DependantChain dependantChain, Map<String, Set<ResolverDefinition>> triggerableDependencies) {
    ForwardBatch forwardBatch = getForwardCommand(dependantChain);
    if (log.isDebugEnabled()) {
      log.debug(
          "Exec ids: {}. Computed triggerable dependencies: {} of {} in call path {}",
          forwardBatch.requestIds(),
          triggerableDependencies.keySet(),
          kryonId,
          forwardBatch.dependantChain());
    }
    Optional<MultiResolverDefinition> multiResolverOpt =
        kryonDefinition
            .multiResolverLogicId()
            .map(
                kryonLogicId ->
                    kryonDefinition
                        .kryonDefinitionRegistry()
                        .logicDefinitionRegistry()
                        .getMultiResolver(kryonLogicId));
    Map<RequestId, String> skippedRequests = forwardBatch.skippedRequests();
    Set<RequestId> executableRequests = forwardBatch.executableRequests().keySet();
    Map<String, Map<Set<RequestId>, ResolverCommand>> commandsByDependency = new LinkedHashMap<>();
    Map<String, Set<RequestId>> requestIdsByDependency = new LinkedHashMap<>();
    if (!skippedRequests.isEmpty()) {
      SkipDependency skip = skip(String.join(", ", skippedRequests.values()));
      for (String depName : triggerableDependencies.keySet()) {
        commandsByDependency
            .computeIfAbsent(depName, _k -> new LinkedHashMap<>())
            .put(unmodifiableSet(skippedRequests.keySet()), skip);
        requestIdsByDependency
            .computeIfAbsent(depName, _k -> new LinkedHashSet<>())
            .addAll(skippedRequests.keySet());
      }
    }

    SetView<String> triggerablesWithNoResolvers =
        Sets.intersection(
            kryonDefinition.dependenciesWithNoResolvers(), triggerableDependencies.keySet());
    for (RequestId requestId : executableRequests) {
      triggerablesWithNoResolvers.forEach(
          depName -> {
            // For such dependencies, trigger them with empty inputs
            commandsByDependency
                .computeIfAbsent(depName, _k -> new LinkedHashMap<>())
                .put(Set.of(requestId), multiExecuteWith(ImmutableList.of(Facets.empty())));
            requestIdsByDependency
                .computeIfAbsent(depName, _k -> new LinkedHashSet<>())
                .add(requestId);
          });
      Set<String> boundFromFacets = new LinkedHashSet<>();
      kryonDefinition
          .dependencyToBoundFacetsMapping()
          .forEach(
              (depName, boundFacets) -> {
                if (triggerableDependencies.containsKey(depName)) {
                  boundFromFacets.addAll(boundFacets);
                }
              });
      Facets facets = getInputsFor(dependantChain, requestId, boundFromFacets);
      multiResolverOpt
          .map(LogicDefinition::logic)
          .map(
              logic -> {
                return logic.resolve(
                    triggerableDependencies.entrySet().stream()
                        .filter(e -> !e.getValue().isEmpty())
                        .map(e -> new DependencyResolutionRequest(e.getKey(), e.getValue()))
                        .toList(),
                    facets);
              })
          .orElse(ImmutableMap.of())
          .forEach(
              (depName, resolverCommand) -> {
                commandsByDependency
                    .computeIfAbsent(depName, _k -> new LinkedHashMap<>())
                    .put(Set.of(requestId), resolverCommand);
                requestIdsByDependency
                    .computeIfAbsent(depName, _k -> new LinkedHashSet<>())
                    .add(requestId);
              });
    }
    for (var entry : commandsByDependency.entrySet()) {
      String depName = entry.getKey();
      var resolverCommandsForDep = entry.getValue();
      triggerDependency(
          depName,
          dependantChain,
          resolverCommandsForDep,
          requestIdsByDependency.getOrDefault(depName, Set.of()));
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
      Set<RequestId> allRequestIds) {
    if (executedDependencies.getOrDefault(dependantChain, Set.of()).contains(depName)) {
      return;
    }
    KryonId depKryonId = kryonDefinition.dependencyKryons().get(depName);
    if (depKryonId == null) {
      throw new AssertionError(
          """
          Could not find kryon mapped to dependency name %s in kryon %s.
          This should not happen and is mostly a bug in the framework.
          """
              .formatted(depName, kryonId));
    }
    Map<RequestId, Facets> inputsByDepReq = new LinkedHashMap<>();
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
            for (Facets facets : resolverCommand.getInputs()) {
              int currentCount = count++;
              RequestId depReqId =
                  requestIdGenerator.newSubRequest(
                      incomingReqId, () -> "%s[%s]".formatted(depName, currentCount));
              depReqsByIncomingReq
                  .computeIfAbsent(incomingReqId, _k -> new LinkedHashSet<>())
                  .add(depReqId);
              inputsByDepReq.put(depReqId, facets);
            }
          }
        }
      }
    }
    executedDependencies.computeIfAbsent(dependantChain, _k -> new LinkedHashSet<>()).add(depName);
    if (log.isDebugEnabled()) {
      skipReasonsByReq.forEach(
          (execId, reason) -> {
            log.debug(
                "Exec Ids: {}. Dependency {} of {} will be skipped due to reason {}",
                execId,
                depName,
                kryonId,
                reason);
          });
    }
    CompletableFuture<BatchResponse> depResponse =
        kryonExecutor.executeCommand(
            new ForwardBatch(
                depKryonId,
                unmodifiableMap(inputsByDepReq),
                dependantChain.extend(kryonId, depName),
                unmodifiableMap(skipReasonsByReq)));

    depResponse.whenComplete(
        (batchResponse, throwable) -> {
          Map<RequestId, Results<Object>> results = new LinkedHashMap<>();
          for (RequestId requestId : allRequestIds) {
            if (throwable != null) {
              results.put(
                  requestId, new Results<>(ImmutableMap.of(Facets.empty(), withError(throwable))));
            } else {
              Set<RequestId> depReqIds = depReqsByIncomingReq.getOrDefault(requestId, Set.of());
              Map<Facets, Errable<Object>> resultsByFacets = new LinkedHashMap<>();
              for (RequestId depReqId : depReqIds) {
                resultsByFacets.put(
                    inputsByDepReq.getOrDefault(depReqId, Facets.empty()),
                    batchResponse.responses().getOrDefault(depReqId, Errable.empty()));
              }
              results.put(requestId, new Results<>(unmodifiableMap(resultsByFacets)));
            }
          }
          enqueueOrExecuteCommand(
              () -> new CallbackBatch(kryonId, depName, unmodifiableMap(results), dependantChain),
              depKryonId,
              kryonDefinition,
              kryonExecutor);
        });
    if (log.isDebugEnabled())
      for (int timeout : List.of(5, 10, 15)) {
        depResponse
            .copy()
            .orTimeout(timeout, SECONDS)
            .whenComplete(
                (_r, throwable) -> {
                  if (throwable instanceof TimeoutException) {
                    log.debug(
                        "KryonId: {}, Dependency: {} on: {} with depChain: {}. Status: Waiting since {} {}",
                        kryonId,
                        depName,
                        depKryonId,
                        dependantChain,
                        timeout,
                        SECONDS);
                  }
                });
      }
    flushDependencyIfNeeded(depName, dependantChain);
  }

  private Optional<CompletableFuture<BatchResponse>> executeOutputLogicIfPossible(
      DependantChain dependantChain) {

    if (outputLogicExecuted.getOrDefault(dependantChain, false)) {
      // Output logic aleady executed
      return Optional.empty();
    }

    ForwardBatch forwardCommand = getForwardCommand(dependantChain);
    // If all the inputs and dependency values needed by the output logic are available, then
    // prepare to run outputLogic
    ImmutableSet<String> inputNames = kryonDefinition.getOutputLogicDefinition().inputNames();
    if (availableInputsByDepChain
        .getOrDefault(dependantChain, ImmutableSet.of())
        .containsAll(inputNames)) { // All the inputs of the kryon logic have data present
      if (forwardCommand.shouldSkip()) {
        return Optional.of(
            failedFuture(new SkippedExecutionException(getSkipMessage(forwardCommand))));
      }
      return Optional.of(
          executeOutputLogic(
              unmodifiableSet(forwardCommand.executableRequests().keySet()), dependantChain));
    }
    return Optional.empty();
  }

  private CompletableFuture<BatchResponse> executeOutputLogic(
      Set<RequestId> requestIds, DependantChain dependantChain) {

    OutputLogicDefinition<Object> outputLogicDefinition =
        kryonDefinition.getOutputLogicDefinition();

    Map<RequestId, OutputLogicFacets> outputLogicInputs = new LinkedHashMap<>();

    for (RequestId requestId : requestIds) {
      outputLogicInputs.put(requestId, getFacetsForOutputLogic(dependantChain, requestId));
    }
    CompletableFuture<BatchResponse> resultForBatch = new CompletableFuture<>();
    Map<RequestId, CompletableFuture<Errable<Object>>> resultFutures =
        executeDecoratedOutputLogic(outputLogicDefinition, outputLogicInputs, dependantChain);

    allOf(resultFutures.values().toArray(CompletableFuture[]::new))
        .whenComplete(
            (unused, throwable) -> {
              Map<RequestId, Errable<Object>> responses = new LinkedHashMap<>();
              for (RequestId requestId : outputLogicInputs.keySet()) {
                CompletableFuture<Errable<Object>> resultFuture =
                    resultFutures.getOrDefault(requestId, new CompletableFuture<>());
                responses.put(requestId, resultFuture.getNow(Errable.empty()));
              }
              resultForBatch.complete(new BatchResponse(unmodifiableMap(responses)));
            });
    outputLogicExecuted.put(dependantChain, true);
    flushDecoratorsIfNeeded(dependantChain);
    return resultForBatch;
  }

  private Map<RequestId, CompletableFuture<Errable<Object>>> executeDecoratedOutputLogic(
      OutputLogicDefinition<Object> outputLogicDefinition,
      Map<RequestId, OutputLogicFacets> inputs,
      DependantChain dependantChain) {
    NavigableSet<OutputLogicDecorator> sortedDecorators = getSortedDecorators(dependantChain);
    OutputLogic<Object> logic = outputLogicDefinition::execute;

    for (OutputLogicDecorator outputLogicDecorator : sortedDecorators) {
      logic = outputLogicDecorator.decorateLogic(logic, outputLogicDefinition);
    }
    OutputLogic<Object> finalLogic = logic;
    Map<RequestId, CompletableFuture<Errable<Object>>> resultsByRequest = new LinkedHashMap<>();
    inputs.forEach(
        (requestId, outputLogicFacets) -> {
          CompletableFuture<@Nullable Object> result;
          try {
            result =
                finalLogic
                    .execute(ImmutableList.of(outputLogicFacets.allFacets()))
                    .values()
                    .iterator()
                    .next();
          } catch (Throwable e) {
            result = failedFuture(e);
          }
          resultsByRequest.put(requestId, result.handle(Errable::errableFrom));
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
    if (outputLogicExecuted.getOrDefault(dependantChain, false)
        || getForwardCommand(dependantChain).shouldSkip()) {
      Iterable<OutputLogicDecorator> reverseSortedDecorators =
          getSortedDecorators(dependantChain)::descendingIterator;
      for (OutputLogicDecorator decorator : reverseSortedDecorators) {
        try {
          decorator.executeCommand(new FlushCommand(dependantChain));
        } catch (Throwable e) {
          log.error(
              """
                  Error while flushing decorator: {}. \
                  This is most probably a bug since decorator methods are not supposed to throw exceptions. \
                  This can cause unpredictable behaviour in the krystal graph execution. \
                  Please fix!""",
              decorator,
              e);
        }
      }
    }
  }

  private Facets getInputsFor(
      DependantChain dependantChain, RequestId requestId, Set<String> boundFrom) {
    Facets resolvableInputs =
        Optional.ofNullable(inputsValueCollector.get(dependantChain))
            .map(ForwardBatch::executableRequests)
            .map(inputsByRequest -> inputsByRequest.get(requestId))
            .orElse(Facets.empty());
    Map<String, CallbackBatch> depValues =
        dependencyValuesCollector.getOrDefault(dependantChain, Map.of());
    Map<String, FacetValue<Object>> inputValues = new LinkedHashMap<>();
    for (String boundFromInput : boundFrom) {
      FacetValue<Object> voe = resolvableInputs.values().get(boundFromInput);
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
    return new Facets(inputValues);
  }

  private OutputLogicFacets getFacetsForOutputLogic(
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
    Facets inputValues = forwardBatch.executableRequests().getOrDefault(requestId, Facets.empty());
    Facets allFacets = Facets.union(depValues, inputValues.values());
    return new OutputLogicFacets(inputValues, allFacets);
  }

  private void collectInputValues(ForwardBatch forwardBatch) {
    if (requestsByDependantChain.putIfAbsent(
            forwardBatch.dependantChain(), forwardBatch.requestIds())
        != null) {
      throw new DuplicateRequestException(
          "Duplicate batch request received for dependant chain %s"
              .formatted(forwardBatch.dependantChain()));
    }
    ImmutableSet<String> givenFacets = kryonDefinition.givenFacets();
    if (inputsValueCollector.putIfAbsent(forwardBatch.dependantChain(), forwardBatch) != null) {
      throw new DuplicateRequestException(
          "Duplicate data for inputs %s of kryon %s in dependant chain %s"
              .formatted(givenFacets, kryonId, forwardBatch.dependantChain()));
    }
    availableInputsByDepChain
        .computeIfAbsent(forwardBatch.dependantChain(), _k -> new LinkedHashSet<>())
        .addAll(givenFacets);
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
