package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.data.Errable.withError;
import static com.flipkart.krystal.krystex.kryon.KryonUtils.enqueueOrExecuteCommand;
import static com.google.common.base.Functions.identity;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.Math.max;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetContainer;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.FacetsBuilder;
import com.flipkart.krystal.data.ImmutableFacets;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.except.IllegalModificationException;
import com.flipkart.krystal.except.SkippedExecutionException;
import com.flipkart.krystal.krystex.LogicDefinition;
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
import com.flipkart.krystal.krystex.resolution.MultiResolver;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.flipkart.krystal.resolution.ResolverCommand;
import com.flipkart.krystal.resolution.ResolverCommand.SkipDependency;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class GranularKryon extends AbstractKryon<GranularCommand, GranuleResponse> {

  private final Map<RequestId, Map<Integer, DependencyKryonExecutions>> dependencyExecutions =
      new LinkedHashMap<>();

  private final Map<RequestId, RequestBuilder<Object>> inputsValueCollector = new LinkedHashMap<>();

  private final Map<RequestId, FacetsBuilder> dependencyValuesCollector = new LinkedHashMap<>();

  private final Map<RequestId, Set<Integer>> availableFacetsByDepChain = new LinkedHashMap<>();

  /** A unique Result future for every requestId. */
  private final Map<RequestId, CompletableFuture<GranuleResponse>> resultsByRequest =
      new LinkedHashMap<>();

  /**
   * A unique {@link CompletableFuture} for every new set of Inputs. This acts as a cache so that
   * the same computation is not repeated multiple times .
   */
  private final Map<Request<Object>, CompletableFuture<@Nullable Object>> resultsCache =
      new LinkedHashMap<>();

  private final Map<RequestId, Boolean> outputLogicExecuted = new LinkedHashMap<>();

  private final Map<RequestId, Optional<SkipGranule>> skipLogicRequested = new LinkedHashMap<>();

  private final Map<RequestId, Map<ResolverDefinition, ResolverCommand>> resolverResults =
      new LinkedHashMap<>();

  private final Set<DependantChain> flushedDependantChain = new LinkedHashSet<>();
  private final Map<DependantChain, Set<RequestId>> requestsByDependantChain =
      new LinkedHashMap<>();
  private final Map<RequestId, DependantChain> dependantChainByRequest = new LinkedHashMap<>();

  private @MonotonicNonNull ImmutableRequest<Object> emptyRequest;
  private @MonotonicNonNull ImmutableFacets emptyFacets;

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
      resultForRequest.completeExceptionally(e);
    }
    return resultForRequest;
  }

  private void executeOutputLogicIfPossible(
      RequestId requestId, CompletableFuture<GranuleResponse> resultForRequest) {
    // If all the inputs and dependency values needed by the output logic are available, then
    // prepare to run outputLogic
    ImmutableSet<Integer> inputIds = kryonDefinition.getOutputLogicDefinition().inputIds();
    if (availableFacetsByDepChain
        .getOrDefault(requestId, Set.of())
        .containsAll(inputIds)) { // All the inputs of the kryon logic have data present
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
    collectInputValues(requestId, forwardGranule.inputIds(), forwardGranule.request());
    execute(requestId, forwardGranule.inputIds());
  }

  private void executeWithDependency(RequestId requestId, CallbackGranule executeWithInput) {
    int dependencyId = executeWithInput.dependencyId();
    ImmutableSet<Integer> inputIds = ImmutableSet.of(dependencyId);
    try {
      dependencyValuesCollector
          .computeIfAbsent(requestId, k -> emptyFacets()._asBuilder())
          ._set(dependencyId, executeWithInput.results());
    } catch (IllegalModificationException e) {
      throw new DuplicateRequestException(
          "Duplicate data for dependency %s of kryon %s in request %s"
              .formatted(dependencyId, kryonId, requestId),
          e);
    }
    availableFacetsByDepChain
        .computeIfAbsent(requestId, _k -> new LinkedHashSet<>())
        .addAll(inputIds);
    execute(requestId, inputIds);
  }

  private void execute(RequestId requestId, ImmutableSet<Integer> newInputNames) {
    FacetsBuilder allFacets =
        dependencyValuesCollector.computeIfAbsent(requestId, k -> emptyFacets()._asBuilder());
    ImmutableSet<Integer> allInputNames = kryonDefinition.facetIds();
    Set<Integer> availableInputs = allFacets._asMap().keySet();
    if (availableInputs.isEmpty()) {
      if (!allInputNames.isEmpty()
          && kryonDefinition.resolverDefinitionsById().isEmpty()
          && !kryonDefinition.dependencyKryons().isEmpty()) {
        executeDependenciesWhenNoResolvers(requestId);
      }
      return;
    }

    executeResolvers(requestId, getPendingResolvers(requestId, newInputNames, availableInputs));
  }

  /**
   * @param requestId The requestId.
   * @param newInputIds The input names for which new values were just made available.
   * @param availableInputs The inputs for which values are available.
   * @return the resolver definitions which need at least one of the provided {@code inputNames} and
   *     all of whose inputs' values are available. i.e. resolvers which should be executed
   *     immediately
   */
  private Set<ResolverDefinition> getPendingResolvers(
      RequestId requestId, ImmutableSet<Integer> newInputIds, Set<Integer> availableInputs) {
    Map<ResolverDefinition, ResolverCommand> resolverResults =
        this.resolverResults.computeIfAbsent(requestId, r -> new LinkedHashMap<>());

    Set<ResolverDefinition> pendingResolvers;
    Set<ResolverDefinition> pendingUnboundResolvers =
        kryonDefinition
            .resolverDefinitionsByInput()
            .getOrDefault(Optional.<Integer>empty(), ImmutableSet.of())
            .stream()
            .filter(
                resolverDefinition -> availableInputs.containsAll(resolverDefinition.boundFrom()))
            .filter(resolverDefinition -> !resolverResults.containsKey(resolverDefinition))
            .collect(toSet());
    pendingResolvers =
        newInputIds.stream()
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

    Optional<LogicDefinition<MultiResolver>> multiResolverOpt =
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
    LogicDefinition<MultiResolver> multiResolver = multiResolverOpt.get();

    Map<Integer, Set<ResolverDefinition>> resolversByDependency =
        pendingResolvers.stream().collect(groupingBy(ResolverDefinition::dependencyId, toSet()));
    Optional<SkipGranule> skipRequested =
        this.skipLogicRequested.getOrDefault(requestId, Optional.empty());
    Map<Integer, ResolverCommand> resolverCommands;
    if (skipRequested.isPresent()) {
      SkipDependency skip =
          ResolverCommand.skip(skipRequested.get().skipDependencyCommand().reason());
      resolverCommands =
          resolversByDependency.keySet().stream().collect(toMap(identity(), _k -> skip));
    } else {
      Facets facets = getFacetsFor(requestId);
      resolverCommands =
          multiResolver
              .logic()
              .resolve(
                  resolversByDependency.entrySet().stream()
                      .map(
                          e ->
                              new DependencyResolutionRequest(
                                  e.getKey(),
                                  e.getValue().stream()
                                      .map(ResolverDefinition::resolverId)
                                      .toList()))
                      .toList(),
                  facets);
    }
    resolverCommands.forEach(
        (depId, resolverCommand) -> {
          handleResolverCommand(
              requestId,
              depId,
              resolversByDependency.getOrDefault(depId, ImmutableSet.of()),
              resolverCommand);
        });
  }

  private void handleResolverCommand(
      RequestId requestId,
      int dependencyId,
      Set<ResolverDefinition> resolverDefinitions,
      ResolverCommand resolverCommand) {
    KryonId depKryonId = getDepKryonId(dependencyId);
    Map<ResolverDefinition, ResolverCommand> resolverResults =
        this.resolverResults.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    resolverDefinitions.forEach(
        resolverDefinition -> resolverResults.put(resolverDefinition, resolverCommand));

    DependencyKryonExecutions dependencyKryonExecutions =
        dependencyExecutions
            .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
            .computeIfAbsent(dependencyId, k -> new DependencyKryonExecutions());
    dependencyKryonExecutions.executedResolvers().addAll(resolverDefinitions);
    if (resolverCommand instanceof SkipDependency) {
      if (!availableFacetsByDepChain.getOrDefault(requestId, Set.of()).contains(dependencyId)) {
        /* This is for the case where for some resolvers the input has already been resolved, but we
        do need to skip them as well, as our current resolver is skipped.*/
        Set<RequestId> requestIdSet =
            new HashSet<>(dependencyKryonExecutions.individualCallResponses().keySet());
        RequestId dependencyRequestId =
            requestIdGenerator.newSubRequest(requestId, () -> "%s[%s]".formatted(dependencyId, 0));
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
                      .extend(kryonId, dependencyId),
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
      ImmutableList<? extends Request<Object>> inputList = resolverCommand.getRequests();
      long executionsInProgress = dependencyKryonExecutions.executionCounter().longValue();
      Map<RequestId, Request<Object>> oldInputs = new LinkedHashMap<>();
      for (int i = 0; i < executionsInProgress; i++) {
        int iFinal = i;
        RequestId rid =
            requestIdGenerator.newSubRequest(
                requestId, () -> "%s[%s]".formatted(dependencyId, iFinal));
        oldInputs.put(
            rid,
            dependencyKryonExecutions
                .individualCallInputs()
                .getOrDefault(rid, getNewDepRequest(dependencyId)));
      }

      long batchSize = max(executionsInProgress, 1);
      long requestCounter = 0;
      for (int j = 0; j < inputList.size(); j++) {
        int jFinal = j;
        Request<Object> facets = inputList.get(j);
        for (int i = 0; i < batchSize; i++) {
          int iFinal = i;
          RequestId dependencyRequestId =
              requestIdGenerator.newSubRequest(
                  requestId, () -> "%s[%s]".formatted(dependencyId, jFinal * batchSize + iFinal));
          RequestId inProgressRequestId;
          if (executionsInProgress > 0) {
            inProgressRequestId =
                requestIdGenerator.newSubRequest(
                    requestId, () -> "%s[%s]".formatted(dependencyId, iFinal));
          } else {
            inProgressRequestId = dependencyRequestId;
          }
          Request<Object> oldInput =
              oldInputs.getOrDefault(inProgressRequestId, getNewDepRequest(dependencyId));
          if (requestCounter >= executionsInProgress) {
            dependencyKryonExecutions.executionCounter().increment();
          }
          RequestBuilder<Object> newFacets;
          if (j == 0) {
            newFacets = facets._asBuilder();
          } else {
            newFacets = oldInput._asBuilder();
            facets._asMap().forEach(newFacets::_set);
          }
          dependencyKryonExecutions.individualCallInputs().put(dependencyRequestId, newFacets);
          dependencyKryonExecutions
              .individualCallResponses()
              .putIfAbsent(
                  dependencyRequestId,
                  kryonExecutor.executeCommand(
                      new ForwardGranule(
                          depKryonId,
                          ImmutableSet.copyOf(newFacets._asMap().keySet()),
                          newFacets,
                          dependantChainByRequest
                              .getOrDefault(
                                  requestId,
                                  kryonDefinition
                                      .kryonDefinitionRegistry()
                                      .getDependantChainsStart())
                              .extend(kryonId, dependencyId),
                          dependencyRequestId)));
        }
        requestCounter += batchSize;
      }
    }
    ImmutableSet<ResolverDefinition> resolverDefinitionsForDependency =
        kryonDefinition
            .resolverDefinitionsByDependencies()
            .getOrDefault(dependencyId, ImmutableSet.of());
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
                      Results<?, Object> results;
                      if (throwable != null) {
                        results =
                            new Results<>(
                                ImmutableList.of(
                                    new RequestResponse<>(
                                        getNewDepRequest(dependencyId), withError(throwable))));
                      } else {
                        results =
                            new Results<>(
                                dependencyKryonExecutions
                                    .individualCallResponses()
                                    .values()
                                    .stream()
                                    .map(cf -> cf.getNow(new GranuleResponse()))
                                    .map(
                                        granuleResponse ->
                                            new RequestResponse<>(
                                                granuleResponse.facets(),
                                                granuleResponse.response()))
                                    .collect(toImmutableList()));
                      }
                      return new CallbackGranule(
                          this.kryonId,
                          dependencyId,
                          results,
                          requestId,
                          getDepChainFor(requestId));
                    },
                    depKryonId,
                    kryonDefinition,
                    kryonExecutor);
              });

      flushDependencyIfNeeded(
          dependencyId,
          dependantChainByRequest.getOrDefault(
              requestId, kryonDefinition.kryonDefinitionRegistry().getDependantChainsStart()));
    }
  }

  private ImmutableFacets emptyFacets() {
    return emptyFacets != null
        ? emptyFacets
        : (emptyFacets =
            kryonDefinition.facetsFromRequest().logic().facetsFromRequest(emptyRequest())._build());
  }

  private Request<Object> emptyRequest() {
    return emptyRequest != null
        ? emptyRequest
        : (emptyRequest = kryonDefinition.createNewRequest().logic().newRequestBuilder()._build());
  }

  private RequestBuilder<Object> getNewDepRequest(int depId) {
    KryonId depKId =
        checkNotNull(
            kryonDefinition.dependencyKryons().get(depId), "Invalid dependency facet id %s", depId);
    KryonDefinition depDef = kryonDefinition.kryonDefinitionRegistry().get(depKId);
    return depDef.createNewRequest().logic().newRequestBuilder();
  }

  private void flushAllDependenciesIfNeeded(DependantChain dependantChain) {
    kryonDefinition
        .dependencyKryons()
        .keySet()
        .forEach(dependencyName -> flushDependencyIfNeeded(dependencyName, dependantChain));
  }

  private void flushDependencyIfNeeded(Integer dependencyId, DependantChain dependantChain) {
    if (!flushedDependantChain.contains(dependantChain)) {
      return;
    }
    Set<RequestId> requestsForDependantChain =
        requestsByDependantChain.getOrDefault(dependantChain, ImmutableSet.of());
    KryonId depKryonId = getDepKryonId(dependencyId);
    ImmutableSet<ResolverDefinition> resolverDefinitionsForDependency =
        kryonDefinition
            .resolverDefinitionsByDependencies()
            .getOrDefault(dependencyId, ImmutableSet.of());
    if (!requestsForDependantChain.isEmpty()
        && requestsForDependantChain.stream()
            .allMatch(
                requestId ->
                    resolverDefinitionsForDependency.equals(
                        this.dependencyExecutions
                            .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
                            .computeIfAbsent(dependencyId, k -> new DependencyKryonExecutions())
                            .executedResolvers()))) {

      kryonExecutor.executeCommand(
          new Flush(depKryonId, dependantChain.extend(kryonId, dependencyId)));
    }
  }

  private KryonId getDepKryonId(int dependencyId) {
    KryonId depKryonId = kryonDefinition.dependencyKryons().get(dependencyId);
    if (depKryonId == null) {
      throw new AssertionError("This is a bug");
    }
    return depKryonId;
  }

  private Facets getFacetsFor(RequestId requestId) {
    return dependencyValuesCollector.computeIfAbsent(requestId, r -> emptyFacets()._asBuilder());
  }

  private void executeDependenciesWhenNoResolvers(RequestId requestId) {
    kryonDefinition
        .dependencyKryons()
        .forEach(
            (depId, depKryonId) -> {
              if (!availableFacetsByDepChain.getOrDefault(requestId, Set.of()).contains(depId)) {
                RequestId dependencyRequestId =
                    requestIdGenerator.newSubRequest(requestId, () -> "%s".formatted(depId));
                CompletableFuture<GranuleResponse> kryonResponse =
                    kryonExecutor.executeCommand(
                        new ForwardGranule(
                            depKryonId,
                            ImmutableSet.of(),
                            getNewDepRequest(depId),
                            getDepChainFor(requestId).extend(kryonId, depId),
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
                                    depId,
                                    new Results<>(
                                        ImmutableList.of(
                                            new RequestResponse<>(emptyRequest(), errable))),
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
    OutputLogicFacets outputLogicFacets = getFacetsForOutputLogic(requestId);
    // Retrieve existing result from cache if result for this set of inputs has already been
    // calculated
    CompletableFuture<@Nullable Object> resultFuture =
        resultsCache.get(outputLogicFacets.request());
    if (resultFuture == null) {
      resultFuture =
          executeDecoratedOutputLogic(
              outputLogicFacets.allFacets(), outputLogicDefinition, requestId);
      resultsCache.put(outputLogicFacets.request(), resultFuture);
    }
    resultFuture
        .handle(Errable::errableFrom)
        .thenAccept(
            value ->
                resultForRequest.complete(new GranuleResponse(outputLogicFacets.request(), value)));
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

  private OutputLogicFacets getFacetsForOutputLogic(RequestId requestId) {
    RequestBuilder<Object> inputValues =
        inputsValueCollector.computeIfAbsent(requestId, _r -> emptyRequest()._asBuilder());
    FacetsBuilder allFacets =
        dependencyValuesCollector.computeIfAbsent(requestId, _r -> emptyFacets()._asBuilder());
    return new OutputLogicFacets(inputValues, allFacets);
  }

  private void collectInputValues(
      RequestId requestId, Set<Integer> inputIds, FacetContainer facets) {
    for (Integer inputId : inputIds) {
      try {
        RequestBuilder<Object> request =
            inputsValueCollector
                .computeIfAbsent(requestId, r -> emptyRequest()._asBuilder())
                ._set(inputId, facets._get(inputId));
        dependencyValuesCollector.computeIfAbsent(
            requestId, r -> kryonDefinition.facetsFromRequest().logic().facetsFromRequest(request));
      } catch (IllegalModificationException e) {
        throw new DuplicateRequestException(
            "Duplicate data for inputs %s of kryon %s in request %s"
                .formatted(inputIds, kryonId, requestId),
            e);
      }
    }
    availableFacetsByDepChain
        .computeIfAbsent(requestId, _k -> new LinkedHashSet<>())
        .addAll(inputIds);
  }

  private record DependencyKryonExecutions(
      LongAdder executionCounter,
      Set<ResolverDefinition> executedResolvers,
      Map<RequestId, Request<Object>> individualCallInputs,
      Map<RequestId, CompletableFuture<GranuleResponse>> individualCallResponses) {

    private DependencyKryonExecutions() {
      this(new LongAdder(), new LinkedHashSet<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }
  }
}
