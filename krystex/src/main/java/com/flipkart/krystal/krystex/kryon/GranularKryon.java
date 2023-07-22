package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.data.ValueOrError.withError;
import static com.flipkart.krystal.krystex.kryon.KryonUtils.enqueueOrExecuteCommand;
import static com.google.common.base.Functions.identity;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.lang.Math.max;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.commands.CallbackGranule;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardGranule;
import com.flipkart.krystal.krystex.commands.GranularCommand;
import com.flipkart.krystal.krystex.commands.SkipGranule;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.ResolverExecStrategy;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.krystex.request.StringReqGenerator;
import com.flipkart.krystal.krystex.resolution.DependencyResolutionRequest;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverCommand;
import com.flipkart.krystal.krystex.resolution.ResolverCommand.SkipDependency;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.flipkart.krystal.utils.SkippedExecutionException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

final class GranularKryon extends AbstractKryon<GranularCommand, GranuleResponse> {

  private final Map<RequestId, Map<String, DependencyKryonExecutions>> dependencyExecutions =
      new LinkedHashMap<>();

  private final Map<RequestId, Map<String, InputValue<Object>>> inputsValueCollector =
      new LinkedHashMap<>();

  private final Map<RequestId, Map<String, Results<Object>>> dependencyValuesCollector =
      new LinkedHashMap<>();

  /** A unique Result future for every requestId. */
  private final Map<RequestId, CompletableFuture<GranuleResponse>> resultsByRequest =
      new LinkedHashMap<>();

  /**
   * A unique {@link CompletableFuture} for every new set of Inputs. This acts as a cache so that
   * the same computation is not repeated multiple times .
   */
  private final Map<Inputs, CompletableFuture<Object>> resultsCache = new LinkedHashMap<>();

  private final Map<RequestId, Boolean> mainLogicExecuted = new LinkedHashMap<>();

  private final Map<RequestId, Optional<SkipGranule>> skipLogicRequested = new LinkedHashMap<>();

  private final Map<RequestId, Map<ResolverDefinition, ResolverCommand>> resolverResults =
      new LinkedHashMap<>();

  private final Set<DependantChain> flushedDependantChain = new LinkedHashSet<>();
  private final Map<DependantChain, Set<RequestId>> requestsByDependantChain =
      new LinkedHashMap<>();
  private final Map<RequestId, DependantChain> dependantChainByRequest = new LinkedHashMap<>();

  GranularKryon(
      KryonDefinition kryonDefinition,
      KryonExecutor kryonExecutor,
      Function<LogicExecutionContext, ImmutableMap<String, MainLogicDecorator>>
          requestScopedDecoratorsSupplier,
      LogicDecorationOrdering logicDecorationOrdering,
      ResolverExecStrategy resolverExecStrategy) {
    super(
        kryonDefinition,
        kryonExecutor,
        requestScopedDecoratorsSupplier,
        logicDecorationOrdering,
        resolverExecStrategy,
        new StringReqGenerator());
  }

  private static SkippedExecutionException skipKryonException(SkipGranule skip) {
    return new SkippedExecutionException(skip.skipDependencyCommand().reason());
  }

  @Override
  public void executeCommand(Flush flushCommand) {
    flushedDependantChain.add(flushCommand.dependantChain());
    flushAllDependenciesIfNeeded(flushCommand.dependantChain());
    flushDecoratorsIfNeeded(flushCommand.dependantChain());
  }

  @Override
  public CompletableFuture<GranuleResponse> executeCommand(GranularCommand kryonCommand) {
    RequestId requestId = kryonCommand.requestId();
    final CompletableFuture<GranuleResponse> resultForRequest =
        resultsByRequest.computeIfAbsent(requestId, r -> new CompletableFuture<>());
    if (resultForRequest.isDone()) {
      // This is possible if this kryon was already skipped, for example.
      // If the result for this requestId is already available, just return and avoid unnecessary
      // computation.
      return resultForRequest;
    }
    try {
      if (kryonCommand instanceof SkipGranule skipGranule) {
        requestsByDependantChain
            .computeIfAbsent(skipGranule.dependantChain(), k -> new LinkedHashSet<>())
            .add(requestId);
        dependantChainByRequest.put(requestId, skipGranule.dependantChain());
        skipLogicRequested.put(requestId, Optional.of(skipGranule));
        return handleSkipDependency(requestId, skipGranule, resultForRequest);
      } else if (kryonCommand instanceof CallbackGranule callbackGranule) {
        executeWithDependency(requestId, callbackGranule);
      } else if (kryonCommand instanceof ForwardGranule forwardGranule) {
        requestsByDependantChain
            .computeIfAbsent(forwardGranule.dependantChain(), k -> new LinkedHashSet<>())
            .add(requestId);
        dependantChainByRequest.computeIfAbsent(requestId, r -> forwardGranule.dependantChain());
        executeWithInputs(requestId, forwardGranule);
      } else {
        throw new UnsupportedOperationException(
            "Unknown type of kryonCommand: %s".formatted(kryonCommand));
      }
      executeMainLogicIfPossible(requestId, resultForRequest);
    } catch (Throwable e) {
      resultForRequest.completeExceptionally(e);
    }
    return resultForRequest;
  }

  private void executeMainLogicIfPossible(
      RequestId requestId, CompletableFuture<GranuleResponse> resultForRequest) {
    // If all the inputs and dependency values are available, then prepare run mainLogic
    MainLogicDefinition<Object> mainLogicDefinition = kryonDefinition.getMainLogicDefinition();
    ImmutableSet<String> inputNames = mainLogicDefinition.inputNames();
    Set<String> collect =
        new LinkedHashSet<>(
            inputsValueCollector.getOrDefault(requestId, ImmutableMap.of()).keySet());
    collect.addAll(dependencyValuesCollector.getOrDefault(requestId, ImmutableMap.of()).keySet());
    if (collect.containsAll(inputNames)) { // All the inputs of the kryon logic have data present
      executeMainLogic(resultForRequest, requestId);
    }
  }

  private CompletableFuture<GranuleResponse> handleSkipDependency(
      RequestId requestId,
      SkipGranule skipGranule,
      CompletableFuture<GranuleResponse> resultForRequest) {

    // Since this kryon is skipped, we need to get all the pending resolvers (irrespective of
    // whether their inputs are available or not) and mark them resolved.
    Set<ResolverDefinition> pendingResolvers =
        resolverDefinitionsByInput.values().stream()
            .flatMap(Collection::stream)
            .filter(
                resolverDefinition ->
                    !this.resolverResults
                        .computeIfAbsent(requestId, r -> new LinkedHashMap<>())
                        .containsKey(resolverDefinition))
            .collect(toSet());

    //    for (ResolverDefinition resolverDefinition : pendingResolvers) {
    //      executeResolver(requestId, resolverDefinition);
    //    }
    executeResolvers(requestId, pendingResolvers);
    resultForRequest.completeExceptionally(skipKryonException(skipGranule));
    return resultForRequest;
  }

  private void flushDecoratorsIfNeeded(DependantChain dependantChain) {
    if (!flushedDependantChain.contains(dependantChain)) {
      return;
    }
    Set<RequestId> requestIds = requestsByDependantChain.get(dependantChain);
    int requestIdExecuted = 0;
    for (RequestId requestId : requestIds) {
      if (mainLogicExecuted.getOrDefault(requestId, false)
          || skipLogicRequested.getOrDefault(requestId, Optional.empty()).isPresent()) {
        requestIdExecuted += 1;
      }
    }
    if (requestIdExecuted == requestIds.size()) {
      Iterable<MainLogicDecorator> reverseSortedDecorators =
          getSortedDecorators(dependantChain)::descendingIterator;
      for (MainLogicDecorator decorator : reverseSortedDecorators) {
        decorator.executeCommand(new FlushCommand(dependantChain));
      }
    }
  }

  private void executeWithInputs(RequestId requestId, ForwardGranule forwardGranule) {
    collectInputValues(requestId, forwardGranule.inputNames(), forwardGranule.values());
    execute(requestId, forwardGranule.inputNames());
  }

  private void executeWithDependency(RequestId requestId, CallbackGranule executeWithInput) {
    String dependencyName = executeWithInput.dependencyName();
    ImmutableSet<String> inputNames = ImmutableSet.of(dependencyName);
    if (dependencyValuesCollector
            .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
            .putIfAbsent(dependencyName, executeWithInput.results())
        != null) {
      throw new DuplicateRequestException(
          "Duplicate data for dependency %s of kryon %s in request %s"
              .formatted(dependencyName, kryonId, requestId));
    }
    execute(requestId, inputNames);
  }

  private void execute(RequestId requestId, ImmutableSet<String> newInputNames) {
    MainLogicDefinition<Object> mainLogicDefinition = kryonDefinition.getMainLogicDefinition();

    Map<String, InputValue<Object>> allInputs =
        inputsValueCollector.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    Map<String, Results<Object>> allDependencies =
        dependencyValuesCollector.computeIfAbsent(requestId, k -> new LinkedHashMap<>());
    ImmutableSet<String> allInputNames = mainLogicDefinition.inputNames();
    Set<String> availableInputs = Sets.union(allInputs.keySet(), allDependencies.keySet());
    if (availableInputs.isEmpty()) {
      if (!allInputNames.isEmpty()
          && kryonDefinition.resolverDefinitions().isEmpty()
          && !kryonDefinition.dependencyKryons().isEmpty()) {
        executeDependenciesWhenNoResolvers(requestId);
      }
      return;
    }

    executeResolvers(requestId, getPendingResolvers(requestId, newInputNames, availableInputs));
  }

  /**
   * @param requestId The requestId.
   * @param newInputNames The input names for which new values were just made available.
   * @param availableInputs The inputs for which values are available.
   * @return the resolver definitions which need at least one of the provided {@code inputNames} and
   *     all of whose inputs' values are available. i.e resolvers which should be executed
   *     immediately
   */
  private Set<ResolverDefinition> getPendingResolvers(
      RequestId requestId, ImmutableSet<String> newInputNames, Set<String> availableInputs) {
    Map<ResolverDefinition, ResolverCommand> resolverResults =
        this.resolverResults.computeIfAbsent(requestId, r -> new LinkedHashMap<>());

    Set<ResolverDefinition> pendingResolvers;
    Set<ResolverDefinition> pendingUnboundResolvers =
        resolverDefinitionsByInput.getOrDefault(Optional.<String>empty(), emptyList()).stream()
            .filter(
                resolverDefinition -> availableInputs.containsAll(resolverDefinition.boundFrom()))
            .filter(resolverDefinition -> !resolverResults.containsKey(resolverDefinition))
            .collect(toSet());
    pendingResolvers =
        newInputNames.stream()
            .flatMap(
                input ->
                    resolverDefinitionsByInput
                        .getOrDefault(Optional.ofNullable(input), ImmutableList.of())
                        .stream()
                        .filter(
                            resolverDefinition ->
                                availableInputs.containsAll(resolverDefinition.boundFrom()))
                        .filter(
                            resolverDefinition -> !resolverResults.containsKey(resolverDefinition)))
            .collect(toSet());
    pendingResolvers.addAll(pendingUnboundResolvers);
    return pendingResolvers;
  }

  private void executeResolvers(RequestId requestId, Set<ResolverDefinition> pendingResolvers) {
    if (ResolverExecStrategy.SINGLE.equals(resolverExecStrategy)) {
      for (ResolverDefinition resolverDefinition : pendingResolvers) {
        executeResolver(requestId, resolverDefinition);
      }
      return;
    }
    if (pendingResolvers.isEmpty()) {
      return;
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
    if (multiResolverOpt.isEmpty()) {
      return;
    }
    MultiResolverDefinition multiResolver = multiResolverOpt.get();

    Map<String, Set<ResolverDefinition>> resolversByDependency =
        pendingResolvers.stream().collect(groupingBy(ResolverDefinition::dependencyName, toSet()));
    Optional<SkipGranule> skipRequested =
        this.skipLogicRequested.getOrDefault(requestId, Optional.empty());
    Map<String, ResolverCommand> resolverCommands;
    if (skipRequested.isPresent()) {
      SkipDependency skip =
          ResolverCommand.skip(skipRequested.get().skipDependencyCommand().reason());
      resolverCommands =
          resolversByDependency.keySet().stream().collect(toMap(identity(), _k -> skip));
    } else {
      Inputs inputs =
          getInputsFor(
              requestId,
              pendingResolvers.stream()
                  .map(ResolverDefinition::boundFrom)
                  .flatMap(Collection::stream)
                  .collect(toSet()));
      resolverCommands =
          multiResolver
              .logic()
              .resolve(
                  resolversByDependency.entrySet().stream()
                      .map(e -> new DependencyResolutionRequest(e.getKey(), e.getValue()))
                      .toList(),
                  inputs);
    }
    resolverCommands.forEach(
        (depName, resolverCommand) -> {
          handleResolverCommand(
              requestId, depName, resolversByDependency.get(depName), resolverCommand);
        });
  }

  private void executeResolver(RequestId requestId, ResolverDefinition resolverDefinition) {
    KryonLogicId kryonLogicId = resolverDefinition.resolverKryonLogicId();
    ResolverCommand resolverCommand;
    Optional<SkipGranule> skipRequested =
        this.skipLogicRequested.getOrDefault(requestId, Optional.empty());
    if (skipRequested.isPresent()) {
      resolverCommand = ResolverCommand.skip(skipRequested.get().skipDependencyCommand().reason());
    } else {
      Inputs inputsForResolver = getInputsForResolver(resolverDefinition, requestId);
      resolverCommand =
          kryonDefinition
              .kryonDefinitionRegistry()
              .logicDefinitionRegistry()
              .getResolver(kryonLogicId)
              .resolve(inputsForResolver);
    }
    String dependencyName = resolverDefinition.dependencyName();
    handleResolverCommand(requestId, dependencyName, Set.of(resolverDefinition), resolverCommand);
  }

  private void handleResolverCommand(
      RequestId requestId,
      String dependencyName,
      Set<ResolverDefinition> resolverDefinitions,
      ResolverCommand resolverCommand) {
    KryonId depKryonId = kryonDefinition.dependencyKryons().get(dependencyName);
    Map<ResolverDefinition, ResolverCommand> resolverResults =
        this.resolverResults.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    resolverDefinitions.forEach(
        resolverDefinition -> resolverResults.put(resolverDefinition, resolverCommand));

    DependencyKryonExecutions dependencyKryonExecutions =
        dependencyExecutions
            .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
            .computeIfAbsent(dependencyName, k -> new DependencyKryonExecutions());
    dependencyKryonExecutions.executedResolvers().addAll(resolverDefinitions);
    if (resolverCommand instanceof SkipDependency) {
      if (dependencyValuesCollector.getOrDefault(requestId, ImmutableMap.of()).get(dependencyName)
          == null) {
        /* This is for the case where for some resolvers the input has already been resolved but we
        do need to skip them as well, as our current resolver is skipped.*/
        Set<RequestId> requestIdSet =
            new HashSet<>(dependencyKryonExecutions.individualCallResponses().keySet());
        RequestId dependencyRequestId =
            requestIdGenerator.newSubRequest(
                requestId, () -> "%s[%s]".formatted(dependencyName, 0));
        /*Skipping Current resolver, as its a skip, we dont need to iterate
         * over fanout requests as the input is empty*/
        requestIdSet.add(dependencyRequestId);
        for (RequestId depRequestId : requestIdSet) {
          SkipGranule skipGranule =
              new SkipGranule(
                  depKryonId,
                  depRequestId,
                  dependantChainByRequest
                      .getOrDefault(
                          requestId,
                          kryonDefinition.kryonDefinitionRegistry().getDependantChainsStart())
                      .extend(kryonId, dependencyName),
                  (SkipDependency) resolverCommand);
          dependencyKryonExecutions
              .individualCallResponses()
              .putIfAbsent(depRequestId, kryonExecutor.executeCommand(skipGranule));
        }
      }
    } else {
      // Since the resolver can return multiple inputs, we have to call the dependency kryon
      // multiple times - each with a different request Id.
      // The current resolver  has triggered a fan-out.
      // So we need multiply the total number of requests to the dependency by n where n is
      // the size of the fan-out triggered by this resolver
      ImmutableList<Inputs> inputList = resolverCommand.getInputs();
      long executionsInProgress = dependencyKryonExecutions.executionCounter().longValue();
      Map<RequestId, Inputs> oldInputs = new LinkedHashMap<>();
      for (int i = 0; i < executionsInProgress; i++) {
        int iFinal = i;
        RequestId rid =
            requestIdGenerator.newSubRequest(
                requestId, () -> "%s[%s]".formatted(dependencyName, iFinal));
        oldInputs.put(
            rid,
            new Inputs(
                dependencyKryonExecutions
                    .individualCallInputs()
                    .getOrDefault(rid, Inputs.empty())
                    .values()));
      }

      long batchSize = max(executionsInProgress, 1);
      int requestCounter = 0;
      for (int j = 0; j < inputList.size(); j++) {
        int jFinal = j;
        Inputs inputs = inputList.get(j);
        for (int i = 0; i < batchSize; i++) {
          int iFinal = i;
          RequestId dependencyRequestId =
              requestIdGenerator.newSubRequest(
                  requestId, () -> "%s[%s]".formatted(dependencyName, jFinal * batchSize + iFinal));
          RequestId inProgressRequestId;
          if (executionsInProgress > 0) {
            inProgressRequestId =
                requestIdGenerator.newSubRequest(
                    requestId, () -> "%s[%s]".formatted(dependencyName, iFinal));
          } else {
            inProgressRequestId = dependencyRequestId;
          }
          Inputs oldInput = oldInputs.getOrDefault(inProgressRequestId, Inputs.empty());
          if (requestCounter >= executionsInProgress) {
            dependencyKryonExecutions.executionCounter().increment();
          }
          Inputs newInputs;
          if (j == 0) {
            newInputs = inputs;
          } else {
            newInputs = Inputs.union(oldInput.values(), inputs.values());
          }
          dependencyKryonExecutions.individualCallInputs().put(dependencyRequestId, newInputs);
          dependencyKryonExecutions
              .individualCallResponses()
              .putIfAbsent(
                  dependencyRequestId,
                  kryonExecutor.executeCommand(
                      new ForwardGranule(
                          depKryonId,
                          newInputs.values().keySet(),
                          newInputs,
                          dependantChainByRequest
                              .getOrDefault(
                                  requestId,
                                  kryonDefinition
                                      .kryonDefinitionRegistry()
                                      .getDependantChainsStart())
                              .extend(kryonId, dependencyName),
                          dependencyRequestId)));
        }
        requestCounter += batchSize;
      }
    }
    ImmutableSet<ResolverDefinition> resolverDefinitionsForDependency =
        this.resolverDefinitionsByDependencies.get(dependencyName);
    if (resolverDefinitionsForDependency.equals(dependencyKryonExecutions.executedResolvers())) {
      allOf(
              dependencyKryonExecutions
                  .individualCallResponses()
                  .values()
                  .toArray(CompletableFuture[]::new))
          .whenComplete(
              (unused, throwable) -> {
                enqueueOrExecuteCommand(
                    () -> {
                      Results<Object> results;
                      if (throwable != null) {
                        results =
                            new Results<>(ImmutableMap.of(Inputs.empty(), withError(throwable)));
                      } else {
                        results =
                            new Results<>(
                                dependencyKryonExecutions
                                    .individualCallResponses()
                                    .values()
                                    .stream()
                                    .map(cf -> cf.getNow(new GranuleResponse()))
                                    .collect(
                                        toImmutableMap(
                                            GranuleResponse::inputs, GranuleResponse::response)));
                      }
                      return new CallbackGranule(
                          this.kryonId,
                          dependencyName,
                          results,
                          requestId,
                          dependantChainByRequest.get(requestId));
                    },
                    depKryonId,
                    kryonDefinition,
                    kryonExecutor);
              });

      flushDependencyIfNeeded(
          dependencyName,
          dependantChainByRequest.getOrDefault(
              requestId, kryonDefinition.kryonDefinitionRegistry().getDependantChainsStart()));
    }
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
    Set<RequestId> requestsForDependantChain =
        requestsByDependantChain.getOrDefault(dependantChain, ImmutableSet.of());
    KryonId depKryonId = kryonDefinition.dependencyKryons().get(dependencyName);
    ImmutableSet<ResolverDefinition> resolverDefinitionsForDependency =
        this.resolverDefinitionsByDependencies.get(dependencyName);
    if (!requestsForDependantChain.isEmpty()
        && requestsForDependantChain.stream()
            .allMatch(
                requestId ->
                    resolverDefinitionsForDependency.equals(
                        this.dependencyExecutions
                            .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
                            .computeIfAbsent(dependencyName, k -> new DependencyKryonExecutions())
                            .executedResolvers()))) {

      kryonExecutor.executeCommand(
          new Flush(depKryonId, dependantChain.extend(kryonId, dependencyName)));
    }
  }

  private Inputs getInputsForResolver(ResolverDefinition resolverDefinition, RequestId requestId) {
    ImmutableSet<String> boundFrom = resolverDefinition.boundFrom();
    return getInputsFor(requestId, boundFrom);
  }

  private Inputs getInputsFor(RequestId requestId, Set<String> boundFrom) {
    Map<String, InputValue<Object>> allInputs =
        inputsValueCollector.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    Map<String, InputValue<Object>> inputValues = new LinkedHashMap<>();
    for (String boundFromInput : boundFrom) {
      InputValue<Object> voe = allInputs.get(boundFromInput);
      if (voe == null) {
        inputValues.put(
            boundFromInput,
            dependencyValuesCollector
                .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
                .get(boundFromInput));
      } else {
        inputValues.put(boundFromInput, voe);
      }
    }
    return new Inputs(inputValues);
  }

  private void executeDependenciesWhenNoResolvers(RequestId requestId) {
    kryonDefinition
        .dependencyKryons()
        .forEach(
            (depName, depKryonId) -> {
              if (!dependencyValuesCollector
                  .getOrDefault(requestId, ImmutableMap.of())
                  .containsKey(depName)) {
                RequestId dependencyRequestId =
                    requestIdGenerator.newSubRequest(requestId, () -> "%s".formatted(depName));
                CompletableFuture<GranuleResponse> kryonResponse =
                    kryonExecutor.executeCommand(
                        new ForwardGranule(
                            depKryonId,
                            ImmutableSet.of(),
                            Inputs.empty(),
                            dependantChainByRequest.get(requestId).extend(kryonId, depName),
                            dependencyRequestId));
                kryonResponse
                    .thenApply(GranuleResponse::response)
                    .whenComplete(
                        (response, throwable) -> {
                          enqueueOrExecuteCommand(
                              () -> {
                                ValueOrError<Object> valueOrError = response;
                                if (throwable != null) {
                                  valueOrError = withError(throwable);
                                }
                                return new CallbackGranule(
                                    this.kryonId,
                                    depName,
                                    new Results<>(ImmutableMap.of(Inputs.empty(), valueOrError)),
                                    requestId,
                                    dependantChainByRequest.get(requestId));
                              },
                              depKryonId,
                              kryonDefinition,
                              kryonExecutor);
                        });
              }
            });
  }

  private void executeMainLogic(
      CompletableFuture<GranuleResponse> resultForRequest, RequestId requestId) {
    MainLogicDefinition<Object> mainLogicDefinition = kryonDefinition.getMainLogicDefinition();
    MainLogicInputs mainLogicInputs = getInputsForMainLogic(requestId);
    // Retrieve existing result from cache if result for this set of inputs has already been
    // calculated
    CompletableFuture<Object> resultFuture = resultsCache.get(mainLogicInputs.providedInputs());
    if (resultFuture == null) {
      resultFuture =
          executeDecoratedMainLogic(
              mainLogicInputs.allInputsAndDependencies(), mainLogicDefinition, requestId);
      resultsCache.put(mainLogicInputs.providedInputs(), resultFuture);
    }
    resultFuture
        .handle(ValueOrError::valueOrError)
        .thenAccept(
            value ->
                resultForRequest.complete(
                    new GranuleResponse(mainLogicInputs.providedInputs(), value)));
    mainLogicExecuted.put(requestId, true);
    flushDecoratorsIfNeeded(dependantChainByRequest.get(requestId));
  }

  private CompletableFuture<Object> executeDecoratedMainLogic(
      Inputs inputs, MainLogicDefinition<Object> mainLogicDefinition, RequestId requestId) {
    SortedSet<MainLogicDecorator> sortedDecorators =
        getSortedDecorators(dependantChainByRequest.get(requestId));
    MainLogic<Object> logic = mainLogicDefinition::execute;

    for (MainLogicDecorator mainLogicDecorator : sortedDecorators) {
      logic = mainLogicDecorator.decorateLogic(logic, mainLogicDefinition);
    }
    return logic.execute(ImmutableList.of(inputs)).get(inputs);
  }

  private MainLogicInputs getInputsForMainLogic(RequestId requestId) {
    Inputs inputValues =
        new Inputs(inputsValueCollector.getOrDefault(requestId, ImmutableMap.of()));
    Map<String, Results<Object>> dependencyValues =
        dependencyValuesCollector.getOrDefault(requestId, ImmutableMap.of());
    Inputs allInputsAndDependencies = Inputs.union(dependencyValues, inputValues.values());
    return new MainLogicInputs(inputValues, allInputsAndDependencies);
  }

  private void collectInputValues(RequestId requestId, Set<String> inputNames, Inputs inputs) {
    for (String inputName : inputNames) {
      if (inputsValueCollector
              .computeIfAbsent(requestId, r -> new LinkedHashMap<>())
              .putIfAbsent(inputName, inputs.getInputValue(inputName))
          != null) {
        throw new DuplicateRequestException(
            "Duplicate data for inputs %s of kryon %s in request %s"
                .formatted(inputNames, kryonId, requestId));
      }
    }
  }

  private record DependencyKryonExecutions(
      LongAdder executionCounter,
      Set<ResolverDefinition> executedResolvers,
      Map<RequestId, Inputs> individualCallInputs,
      Map<RequestId, CompletableFuture<GranuleResponse>> individualCallResponses) {

    private DependencyKryonExecutions() {
      this(new LongAdder(), new LinkedHashSet<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }
  }
}
