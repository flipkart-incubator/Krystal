package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.data.Errable.withError;
import static com.flipkart.krystal.except.StackTracelessException.stackTracelessWrap;
import static com.flipkart.krystal.krystex.kryon.KryonUtils.enqueueOrExecuteCommand;
import static com.google.common.base.Functions.identity;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.lang.Math.max;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.except.SkippedExecutionException;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.CallbackGranule;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardGranule;
import com.flipkart.krystal.krystex.commands.GranularCommand;
import com.flipkart.krystal.krystex.commands.SkipGranule;
import com.flipkart.krystal.krystex.logicdecoration.FlushCommand;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.krystex.request.StringReqGenerator;
import com.flipkart.krystal.krystex.resolution.DependencyResolutionRequest;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverCommand;
import com.flipkart.krystal.krystex.resolution.ResolverCommand.SkipDependency;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
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
import org.checkerframework.checker.nullness.qual.Nullable;

final class GranularKryon extends AbstractKryon<GranularCommand, GranuleResponse> {

  private final Map<RequestId, Map<String, DependencyKryonExecutions>> dependencyExecutions =
      new LinkedHashMap<>();

  private final Map<RequestId, Map<String, FacetValue<Object>>> inputsValueCollector =
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
  private final Map<Facets, CompletableFuture<@Nullable Object>> resultsCache =
      new LinkedHashMap<>();

  private final Map<RequestId, Boolean> outputLogicExecuted = new LinkedHashMap<>();

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
      Function<LogicExecutionContext, ImmutableMap<String, OutputLogicDecorator>>
          requestScopedDecoratorsSupplier,
      LogicDecorationOrdering logicDecorationOrdering) {
    super(
        kryonDefinition,
        kryonExecutor,
        requestScopedDecoratorsSupplier,
        logicDecorationOrdering,
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
      executeOutputLogicIfPossible(requestId, resultForRequest);
    } catch (Throwable e) {
      resultForRequest.completeExceptionally(stackTracelessWrap(e));
    }
    return resultForRequest;
  }

  private void executeOutputLogicIfPossible(
      RequestId requestId, CompletableFuture<GranuleResponse> resultForRequest) {
    // If all the inputs and dependency values needed by the output logic are available, then
    // prepare to run outputLogic
    ImmutableSet<String> inputNames = kryonDefinition.getOutputLogicDefinition().inputNames();
    Set<String> collect =
        new LinkedHashSet<>(
            inputsValueCollector.getOrDefault(requestId, ImmutableMap.of()).keySet());
    collect.addAll(dependencyValuesCollector.getOrDefault(requestId, ImmutableMap.of()).keySet());
    if (collect.containsAll(inputNames)) { // All the inputs of the kryon logic have data present
      executeOutputLogic(resultForRequest, requestId);
    }
  }

  private CompletableFuture<GranuleResponse> handleSkipDependency(
      RequestId requestId,
      SkipGranule skipGranule,
      CompletableFuture<GranuleResponse> resultForRequest) {

    // Since this kryon is skipped, we need to get all the pending resolvers (irrespective of
    // whether their inputs are available or not) and mark them resolved.
    Set<ResolverDefinition> pendingResolvers =
        kryonDefinition.resolverDefinitionsByInput().values().stream()
            .flatMap(Collection::stream)
            .filter(
                resolverDefinition ->
                    !this.resolverResults
                        .computeIfAbsent(requestId, r -> new LinkedHashMap<>())
                        .containsKey(resolverDefinition))
            .collect(toSet());

    executeResolvers(requestId, pendingResolvers);
    resultForRequest.completeExceptionally(skipKryonException(skipGranule));
    return resultForRequest;
  }

  private void flushDecoratorsIfNeeded(DependantChain dependantChain) {
    if (!flushedDependantChain.contains(dependantChain)) {
      return;
    }
    Set<RequestId> requestIds =
        requestsByDependantChain.getOrDefault(dependantChain, ImmutableSet.of());
    int requestIdExecuted = 0;
    for (RequestId requestId : requestIds) {
      if (outputLogicExecuted.getOrDefault(requestId, false)
          || skipLogicRequested.getOrDefault(requestId, Optional.empty()).isPresent()) {
        requestIdExecuted += 1;
      }
    }
    if (requestIdExecuted == requestIds.size()) {
      Iterable<OutputLogicDecorator> reverseSortedDecorators =
          getSortedDecorators(dependantChain)::descendingIterator;
      for (OutputLogicDecorator decorator : reverseSortedDecorators) {
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
    Map<String, FacetValue<Object>> allInputs =
        inputsValueCollector.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    Map<String, Results<Object>> allDependencies =
        dependencyValuesCollector.computeIfAbsent(requestId, k -> new LinkedHashMap<>());
    ImmutableSet<String> allInputNames = kryonDefinition.facetNames();
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
   *     all of whose inputs' values are available. i.e. resolvers which should be executed
   *     immediately
   */
  private Set<ResolverDefinition> getPendingResolvers(
      RequestId requestId, ImmutableSet<String> newInputNames, Set<String> availableInputs) {
    Map<ResolverDefinition, ResolverCommand> resolverResults =
        this.resolverResults.computeIfAbsent(requestId, r -> new LinkedHashMap<>());

    Set<ResolverDefinition> pendingResolvers;
    Set<ResolverDefinition> pendingUnboundResolvers =
        kryonDefinition
            .resolverDefinitionsByInput()
            .getOrDefault(Optional.<String>empty(), ImmutableSet.of())
            .stream()
            .filter(
                resolverDefinition -> availableInputs.containsAll(resolverDefinition.boundFrom()))
            .filter(resolverDefinition -> !resolverResults.containsKey(resolverDefinition))
            .collect(toSet());
    pendingResolvers =
        newInputNames.stream()
            .flatMap(
                input ->
                    kryonDefinition
                        .resolverDefinitionsByInput()
                        .getOrDefault(Optional.ofNullable(input), ImmutableSet.of())
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
      Facets facets =
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
                  facets);
    }
    resolverCommands.forEach(
        (depName, resolverCommand) -> {
          handleResolverCommand(
              requestId,
              depName,
              resolversByDependency.getOrDefault(depName, ImmutableSet.of()),
              resolverCommand);
        });
  }

  private void handleResolverCommand(
      RequestId requestId,
      String dependencyName,
      Set<ResolverDefinition> resolverDefinitions,
      ResolverCommand resolverCommand) {
    KryonId depKryonId = getDepKryonId(dependencyName);
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
        /* This is for the case where for some resolvers the input has already been resolved, but we
        do need to skip them as well, as our current resolver is skipped.*/
        Set<RequestId> requestIdSet =
            new HashSet<>(dependencyKryonExecutions.individualCallResponses().keySet());
        RequestId dependencyRequestId =
            requestIdGenerator.newSubRequest(
                requestId, () -> "%s[%s]".formatted(dependencyName, 0));
        /*Skipping Current resolver, as it's a skip, we don't need to iterate
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
      ImmutableList<Facets> inputList = resolverCommand.getInputs();
      long executionsInProgress = dependencyKryonExecutions.executionCounter().longValue();
      Map<RequestId, Facets> oldInputs = new LinkedHashMap<>();
      for (int i = 0; i < executionsInProgress; i++) {
        int iFinal = i;
        RequestId rid =
            requestIdGenerator.newSubRequest(
                requestId, () -> "%s[%s]".formatted(dependencyName, iFinal));
        oldInputs.put(
            rid,
            new Facets(
                dependencyKryonExecutions
                    .individualCallInputs()
                    .getOrDefault(rid, Facets.empty())
                    .values()));
      }

      long batchSize = max(executionsInProgress, 1);
      long requestCounter = 0;
      for (int j = 0; j < inputList.size(); j++) {
        int jFinal = j;
        Facets facets = inputList.get(j);
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
          Facets oldInput = oldInputs.getOrDefault(inProgressRequestId, Facets.empty());
          if (requestCounter >= executionsInProgress) {
            dependencyKryonExecutions.executionCounter().increment();
          }
          Facets newFacets;
          if (j == 0) {
            newFacets = facets;
          } else {
            newFacets = Facets.union(oldInput.values(), facets.values());
          }
          dependencyKryonExecutions.individualCallInputs().put(dependencyRequestId, newFacets);
          dependencyKryonExecutions
              .individualCallResponses()
              .putIfAbsent(
                  dependencyRequestId,
                  kryonExecutor.executeCommand(
                      new ForwardGranule(
                          depKryonId,
                          newFacets.values().keySet(),
                          newFacets,
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
        kryonDefinition
            .resolverDefinitionsByDependencies()
            .getOrDefault(dependencyName, ImmutableSet.of());
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
                            new Results<>(ImmutableMap.of(Facets.empty(), withError(throwable)));
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
                                            GranuleResponse::facets, GranuleResponse::response)));
                      }
                      return new CallbackGranule(
                          this.kryonId,
                          dependencyName,
                          results,
                          requestId,
                          getDepChainFor(requestId));
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
    KryonId depKryonId = getDepKryonId(dependencyName);
    ImmutableSet<ResolverDefinition> resolverDefinitionsForDependency =
        kryonDefinition
            .resolverDefinitionsByDependencies()
            .getOrDefault(dependencyName, ImmutableSet.of());
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

  private KryonId getDepKryonId(String dependencyName) {
    KryonId depKryonId = kryonDefinition.dependencyKryons().get(dependencyName);
    if (depKryonId == null) {
      throw new AssertionError("This is a bug");
    }
    return depKryonId;
  }

  private Facets getInputsFor(RequestId requestId, Set<String> boundFrom) {
    Map<String, FacetValue<Object>> allInputs =
        inputsValueCollector.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    Map<String, FacetValue<Object>> inputValues = new LinkedHashMap<>();
    for (String boundFromInput : boundFrom) {
      FacetValue<Object> voe = allInputs.get(boundFromInput);
      if (voe == null) {
        inputValues.put(
            boundFromInput,
            dependencyValuesCollector
                .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
                .getOrDefault(boundFromInput, Results.empty()));
      } else {
        inputValues.put(boundFromInput, voe);
      }
    }
    return new Facets(inputValues);
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
                            Facets.empty(),
                            getDepChainFor(requestId).extend(kryonId, depName),
                            dependencyRequestId));
                kryonResponse
                    .thenApply(GranuleResponse::response)
                    .whenComplete(
                        (response, throwable) -> {
                          enqueueOrExecuteCommand(
                              () -> {
                                Errable<Object> errable = response;
                                if (throwable != null) {
                                  errable = withError(throwable);
                                }
                                return new CallbackGranule(
                                    this.kryonId,
                                    depName,
                                    new Results<>(ImmutableMap.of(Facets.empty(), errable)),
                                    requestId,
                                    getDepChainFor(requestId));
                              },
                              depKryonId,
                              kryonDefinition,
                              kryonExecutor);
                        });
              }
            });
  }

  private void executeOutputLogic(
      CompletableFuture<GranuleResponse> resultForRequest, RequestId requestId) {
    OutputLogicDefinition<Object> outputLogicDefinition =
        kryonDefinition.getOutputLogicDefinition();
    OutputLogicFacets outputLogicFacets = getInputsForOutputLogic(requestId);
    // Retrieve existing result from cache if result for this set of inputs has already been
    // calculated
    CompletableFuture<@Nullable Object> resultFuture =
        resultsCache.get(outputLogicFacets.providedFacets());
    if (resultFuture == null) {
      resultFuture =
          executeDecoratedOutputLogic(
              outputLogicFacets.allFacets(), outputLogicDefinition, requestId);
      resultsCache.put(outputLogicFacets.providedFacets(), resultFuture);
    }
    resultFuture
        .handle(Errable::errableFrom)
        .thenAccept(
            value ->
                resultForRequest.complete(
                    new GranuleResponse(outputLogicFacets.providedFacets(), value)));
    outputLogicExecuted.put(requestId, true);
    flushDecoratorsIfNeeded(getDepChainFor(requestId));
  }

  private CompletableFuture<@Nullable Object> executeDecoratedOutputLogic(
      Facets facets, OutputLogicDefinition<Object> outputLogicDefinition, RequestId requestId) {
    SortedSet<OutputLogicDecorator> sortedDecorators =
        getSortedDecorators(getDepChainFor(requestId));
    OutputLogic<Object> logic = outputLogicDefinition::execute;

    for (OutputLogicDecorator outputLogicDecorator : sortedDecorators) {
      logic = outputLogicDecorator.decorateLogic(logic, outputLogicDefinition);
    }
    return Optional.ofNullable(logic.execute(ImmutableList.of(facets)).get(facets))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Output logic "
                        + outputLogicDefinition.kryonLogicId()
                        + " did not return a future for inputs "
                        + facets));
  }

  private DependantChain getDepChainFor(RequestId requestId) {
    return Optional.ofNullable(dependantChainByRequest.get(requestId))
        .orElseThrow(() -> new AssertionError("This is a bug"));
  }

  private OutputLogicFacets getInputsForOutputLogic(RequestId requestId) {
    Facets inputValues =
        new Facets(inputsValueCollector.getOrDefault(requestId, ImmutableMap.of()));
    Map<String, Results<Object>> dependencyValues =
        dependencyValuesCollector.getOrDefault(requestId, ImmutableMap.of());
    Facets allFacets = Facets.union(dependencyValues, inputValues.values());
    return new OutputLogicFacets(inputValues, allFacets);
  }

  private void collectInputValues(RequestId requestId, Set<String> inputNames, Facets facets) {
    for (String inputName : inputNames) {
      if (inputsValueCollector
              .computeIfAbsent(requestId, r -> new LinkedHashMap<>())
              .putIfAbsent(inputName, facets.getInputValue(inputName))
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
      Map<RequestId, Facets> individualCallInputs,
      Map<RequestId, CompletableFuture<GranuleResponse>> individualCallResponses) {

    private DependencyKryonExecutions() {
      this(new LongAdder(), new LinkedHashSet<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }
  }
}
