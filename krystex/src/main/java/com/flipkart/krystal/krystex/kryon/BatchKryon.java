package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.data.Errable.nil;
import static com.flipkart.krystal.data.Errable.withError;
import static com.flipkart.krystal.krystex.kryon.KryonUtils.enqueueOrExecuteCommand;
import static com.flipkart.krystal.resolution.ResolverCommand.computedRequests;
import static com.flipkart.krystal.resolution.ResolverCommand.skip;
import static com.flipkart.krystal.utils.Futures.linkFutures;
import static com.google.common.base.Functions.identity;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.FacetsBuilder;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.data.Results;
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
import com.flipkart.krystal.krystex.resolution.MultiResolver;
import com.flipkart.krystal.resolution.ResolverCommand;
import com.flipkart.krystal.resolution.ResolverCommand.SkipDependency;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
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

  private final Map<DependantChain, Set<Integer>> availableFacetsByDepChain = new LinkedHashMap<>();

  private final Map<DependantChain, Map<RequestId, FacetsBuilder>> facetsCollector =
      new LinkedHashMap<>();

  private final Map<DependantChain, ForwardBatch> inputsValueCollector = new LinkedHashMap<>();

  private final Map<DependantChain, Map<Integer, CallbackBatch>> dependencyValuesCollector =
      new LinkedHashMap<>();

  /** A unique Result future for every dependant chain. */
  private final Map<DependantChain, CompletableFuture<BatchResponse>> resultsByDepChain =
      new LinkedHashMap<>();

  /**
   * A unique {@link CompletableFuture} for every new set of Inputs. This acts as a cache so that
   * the same computation is not repeated multiple times .
   */
  private final Map<Request<Object>, CompletableFuture<@Nullable Object>> resultsCache =
      new LinkedHashMap<>();

  private final Map<DependantChain, Set<Integer>> executedDependencies = new LinkedHashMap<>();

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
      if (kryonCommand instanceof ForwardBatch forwardBatch) {
        collectInputValues(forwardBatch);
      } else if (kryonCommand instanceof CallbackBatch callbackBatch) {
        collectDependencyValues(callbackBatch);
      }
      triggerDependencies(
          dependantChain, getTriggerableDependencies(dependantChain, kryonCommand.facetIds()));

      Optional<CompletableFuture<BatchResponse>> outputLogicFuture =
          executeOutputLogicIfPossible(dependantChain);
      outputLogicFuture.ifPresent(f -> linkFutures(f, resultForDepChain));
    } catch (Throwable e) {
      resultForDepChain.completeExceptionally(e);
    }
    return resultForDepChain;
  }

  private Map<Integer, ImmutableList<Integer>> getTriggerableDependencies(
      DependantChain dependantChain, Set<Integer> newInputIds) {
    Set<Integer> availableInputs = availableFacetsByDepChain.getOrDefault(dependantChain, Set.of());
    Set<Integer> executedDeps = executedDependencies.getOrDefault(dependantChain, Set.of());

    return Stream.concat(
            Stream.concat(
                    Stream.of(Optional.<Integer>empty()), newInputIds.stream().map(Optional::of))
                .map(
                    key ->
                        kryonDefinition
                            .resolverDefinitionsByInput()
                            .getOrDefault(key, ImmutableSet.of()))
                .flatMap(Collection::stream)
                .map(ResolverDefinition::dependencyId),
            kryonDefinition.dependenciesWithNoResolvers().stream())
        .distinct()
        .filter(depName -> !executedDeps.contains(depName))
        .filter(
            depName ->
                kryonDefinition
                    .resolverDefinitionsByDependencies()
                    .getOrDefault(depName, ImmutableSet.of())
                    .stream()
                    .map(ResolverDefinition::boundFrom)
                    .flatMap(Collection::stream)
                    .allMatch(availableInputs::contains))
        .collect(
            toMap(
                identity(),
                depFacetId ->
                    kryonDefinition
                        .resolverDefinitionsByDependencies()
                        .getOrDefault(depFacetId, ImmutableSet.of())
                        .stream()
                        .map(ResolverDefinition::resolverId)
                        .collect(toImmutableList())));
  }

  private void triggerDependencies(
      DependantChain dependantChain,
      Map</*depFacetId*/ Integer, ImmutableList</*resolverId*/ Integer>> triggerableDependencies) {
    ForwardBatch forwardBatch = getForwardCommand(dependantChain);

    Optional<LogicDefinition<MultiResolver>> multiResolverOpt =
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
    Map<Integer, Map<Set<RequestId>, ResolverCommand>> commandsByDependency = new LinkedHashMap<>();
    if (!skippedRequests.isEmpty()) {
      SkipDependency skip = skip(String.join(", ", skippedRequests.values()));
      for (Integer depName : triggerableDependencies.keySet()) {
        commandsByDependency
            .computeIfAbsent(depName, _k -> new LinkedHashMap<>())
            .put(skippedRequests.keySet(), skip);
      }
    }

    Set<Integer> dependenciesWithNoResolvers =
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
                .put(Set.of(requestId), computedRequests(ImmutableList.of(emptyRequest())));
          });
      Facets facets = getFacetsFor(dependantChain, requestId);
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
              });
    }
    for (var entry : commandsByDependency.entrySet()) {
      Integer depId = entry.getKey();
      var resolverCommandsForDep = entry.getValue();
      triggerDependency(
          depId,
          dependantChain,
          resolverCommandsForDep,
          triggerableDependencies.getOrDefault(depId, ImmutableList.of()));
    }
  }

  private FacetsBuilder facetsFromRequest(Request<Object> request) {
    return kryonDefinition.facetsFromRequest().logic().facetsFromRequest(request);
  }

  private FacetsBuilder emptyFacets() {
    return kryonDefinition.facetsFromRequest().logic().facetsFromRequest(emptyRequest());
  }

  private <T> ImmutableRequest<T> emptyRequest() {
    //noinspection unchecked
    return (ImmutableRequest<T>)
        kryonDefinition.createNewRequest().logic().newRequestBuilder()._build();
  }

  private ForwardBatch getForwardCommand(DependantChain dependantChain) {
    ForwardBatch forwardBatch = inputsValueCollector.get(dependantChain);
    if (forwardBatch == null) {
      throw new IllegalArgumentException("Missing Forward command. This should not be possible.");
    }
    return forwardBatch;
  }

  private void triggerDependency(
      int depId,
      DependantChain dependantChain,
      Map<Set<RequestId>, ResolverCommand> resolverCommandsByReq,
      ImmutableList<Integer> resolverIds) {
    KryonId depKryonId = kryonDefinition.dependencyKryons().get(depId);
    if (depKryonId == null) {
      throw new AssertionError("This is a bug.");
    }
    Map<RequestId, ImmutableRequest<Object>> inputsByDepReq = new LinkedHashMap<>();
    Map<RequestId, String> skipReasonsByReq = new LinkedHashMap<>();
    Map<RequestId, Set<RequestId>> depReqsByIncomingReq = new LinkedHashMap<>();
    for (var entry : resolverCommandsByReq.entrySet()) {
      Set<RequestId> incomingReqIds = entry.getKey();
      ResolverCommand resolverCommand = entry.getValue();
      if (resolverCommand instanceof SkipDependency skipDependency) {
        RequestId depReqId =
            requestIdGenerator.newSubRequest(
                incomingReqIds.iterator().next(), () -> "%s[skip]".formatted(depId));
        incomingReqIds.forEach(
            incomingReqId ->
                depReqsByIncomingReq
                    .computeIfAbsent(incomingReqId, _k -> new LinkedHashSet<>())
                    .add(depReqId));
        skipReasonsByReq.put(depReqId, skipDependency.reason());
      } else {
        int count = 0;
        for (RequestId incomingReqId : incomingReqIds) {
          if (resolverCommand.getRequests().isEmpty()) {
            RequestId depReqId =
                requestIdGenerator.newSubRequest(incomingReqId, () -> "%s[skip]".formatted(depId));
            skipReasonsByReq.put(
                depReqId, "Resolvers for dependency %s resolved to empty list".formatted(depId));
          } else {
            for (Request<Object> request : resolverCommand.getRequests()) {
              int currentCount = count++;
              RequestId depReqId =
                  requestIdGenerator.newSubRequest(
                      incomingReqId, () -> "%s[%s]".formatted(depId, currentCount));
              depReqsByIncomingReq
                  .computeIfAbsent(incomingReqId, _k -> new LinkedHashSet<>())
                  .add(depReqId);
              inputsByDepReq.put(depReqId, request._build());
            }
          }
        }
      }
    }
    executedDependencies.computeIfAbsent(dependantChain, _k -> new LinkedHashSet<>()).add(depId);
    CompletableFuture<BatchResponse> depResponse =
        kryonExecutor.executeCommand(
            new ForwardBatch(
                depKryonId,
                resolverIds.stream()
                    .map(rid -> checkNotNull(kryonDefinition.resolverDefinitionsById().get(rid)))
                    .map(ResolverDefinition::resolvedInputs)
                    .flatMap(Collection::stream)
                    .collect(toImmutableSet()),
                ImmutableMap.copyOf(inputsByDepReq),
                dependantChain.extend(kryonId, depId),
                ImmutableMap.copyOf(skipReasonsByReq)));

    depResponse.whenComplete(
        (batchResponse, throwable) -> {
          Set<RequestId> requestIds =
              resolverCommandsByReq.keySet().stream().flatMap(Collection::stream).collect(toSet());
          ImmutableMap<RequestId, Results<?, Object>> results =
              requestIds.stream()
                  .collect(
                      toImmutableMap(
                          identity(),
                          requestId -> {
                            if (throwable != null) {
                              return new Results<>(
                                  ImmutableList.of(
                                      new RequestResponse<>(emptyRequest(), withError(throwable))));
                            } else {
                              Set<RequestId> depReqIds =
                                  depReqsByIncomingReq.getOrDefault(requestId, Set.of());
                              ImmutableList<RequestResponse<Request<Object>, Object>> collect =
                                  depReqIds.stream()
                                      .map(
                                          depReqId -> {
                                            return new RequestResponse<>(
                                                (Request<Object>)
                                                    inputsByDepReq.getOrDefault(
                                                        depReqId, emptyRequest()),
                                                batchResponse
                                                    .responses()
                                                    .getOrDefault(depReqId, nil()));
                                          })
                                      .collect(toImmutableList());
                              return new Results<>(collect);
                            }
                          }));

          enqueueOrExecuteCommand(
              () -> new CallbackBatch(kryonId, depId, results, dependantChain),
              depKryonId,
              kryonDefinition,
              kryonExecutor);
        });
    flushDependencyIfNeeded(depId, dependantChain);
  }

  private Optional<CompletableFuture<BatchResponse>> executeOutputLogicIfPossible(
      DependantChain dependantChain) {
    ForwardBatch forwardCommand = getForwardCommand(dependantChain);
    // If all the inputs and dependency values needed by the output logic are available, then
    // prepare to run outputLogic
    ImmutableSet<Integer> facetIds = kryonDefinition.getOutputLogicDefinition().inputIds();
    if (availableFacetsByDepChain
        .getOrDefault(dependantChain, ImmutableSet.of())
        .containsAll(facetIds)) { // All the inputs of the kryon logic have data present
      if (forwardCommand.shouldSkip()) {
        return Optional.of(
            failedFuture(new SkippedExecutionException(getSkipMessage(forwardCommand))));
      }
      return Optional.of(
          executeOutputLogic(forwardCommand.executableRequests().keySet(), dependantChain));
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
    Map<RequestId, CompletableFuture<Errable<Object>>> results =
        executeDecoratedOutputLogic(outputLogicDefinition, outputLogicInputs, dependantChain);

    allOf(results.values().toArray(CompletableFuture[]::new))
        .whenComplete(
            (unused, throwable) -> {
              resultForBatch.complete(
                  new BatchResponse(
                      outputLogicInputs.keySet().stream()
                          .collect(
                              toImmutableMap(
                                  identity(),
                                  requestId ->
                                      results
                                          .getOrDefault(requestId, new CompletableFuture<>())
                                          .getNow(nil())))));
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
          // Retrieve existing result from cache if result for this set of inputs has already been
          // calculated
          CompletableFuture<@Nullable Object> cachedResult =
              resultsCache.get(outputLogicFacets.request());
          if (cachedResult == null) {
            try {
              cachedResult =
                  finalLogic
                      .execute(ImmutableList.of(outputLogicFacets.allFacets()))
                      .values()
                      .iterator()
                      .next();
            } catch (Exception e) {
              cachedResult = failedFuture(e);
            }
            resultsCache.put(outputLogicFacets.request(), cachedResult);
          }
          resultsByRequest.put(requestId, cachedResult.handle(Errable::errableFrom));
        });
    return resultsByRequest;
  }

  private void flushAllDependenciesIfNeeded(DependantChain dependantChain) {
    kryonDefinition
        .dependencyKryons()
        .keySet()
        .forEach(dependencyName -> flushDependencyIfNeeded(dependencyName, dependantChain));
  }

  private void flushDependencyIfNeeded(int dependencyId, DependantChain dependantChain) {
    if (!flushedDependantChain.contains(dependantChain)) {
      return;
    }
    if (executedDependencies.getOrDefault(dependantChain, Set.of()).contains(dependencyId)) {
      kryonExecutor.executeCommand(
          new Flush(
              Optional.ofNullable(kryonDefinition.dependencyKryons().get(dependencyId))
                  .orElseThrow(
                      () ->
                          new AssertionError(
                              "Could not find KryonId for dependency "
                                  + dependencyId
                                  + ". This is a bug")),
              dependantChain.extend(kryonId, dependencyId)));
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
        decorator.executeCommand(new FlushCommand(dependantChain));
      }
    }
  }

  private Facets getFacetsFor(DependantChain dependantChain, RequestId requestId) {
    return facetsCollector
        .getOrDefault(dependantChain, Map.of())
        .getOrDefault(requestId, emptyFacets());
  }

  private OutputLogicFacets getFacetsForOutputLogic(
      DependantChain dependantChain, RequestId requestId) {
    ForwardBatch forwardBatch = getForwardCommand(dependantChain);
    ImmutableMap<Integer, Results<?, Object>> depValues =
        dependencyValuesCollector
            .getOrDefault(dependantChain, ImmutableMap.of())
            .entrySet()
            .stream()
            .collect(
                toImmutableMap(
                    Entry::getKey,
                    e -> e.getValue().resultsByRequest().getOrDefault(requestId, Results.empty())));
    Request<Object> inputValues =
        forwardBatch.executableRequests().getOrDefault(requestId, emptyRequest());
    FacetsBuilder allFacets =
        kryonDefinition.facetsFromRequest().logic().facetsFromRequest(inputValues);
    depValues.forEach(allFacets::_set);

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
    ImmutableSet<Integer> inputIds = forwardBatch.facetIds();
    if (inputsValueCollector.putIfAbsent(forwardBatch.dependantChain(), forwardBatch) != null) {
      throw new DuplicateRequestException(
          "Duplicate data for inputs %s of kryon %s in dependant chain %s"
              .formatted(inputIds, kryonId, forwardBatch.dependantChain()));
    }
    SetView<Integer> resolvableInputNames =
        Sets.difference(kryonDefinition.facetIds(), kryonDefinition.dependencyKryons().keySet());
    if (!inputIds.containsAll(resolvableInputNames)) {
      throw new IllegalArgumentException(
          "Did not receive inputs " + Sets.difference(resolvableInputNames, inputIds));
    }

    availableFacetsByDepChain
        .computeIfAbsent(forwardBatch.dependantChain(), _k -> new LinkedHashSet<>())
        .addAll(inputIds);
    forwardBatch
        .executableRequests()
        .forEach(
            (requestId, request) -> {
              facetsCollector
                  .computeIfAbsent(forwardBatch.dependantChain(), _d -> new LinkedHashMap<>())
                  .put(requestId, facetsFromRequest(request));
            });
  }

  private static String getSkipMessage(ForwardBatch forwardBatch) {
    return String.join(", ", forwardBatch.skippedRequests().values());
  }

  private void collectDependencyValues(CallbackBatch callbackBatch) {
    int dependencyId = callbackBatch.dependencyId();
    availableFacetsByDepChain
        .computeIfAbsent(callbackBatch.dependantChain(), _k -> new LinkedHashSet<>())
        .add(dependencyId);
    if (dependencyValuesCollector
            .computeIfAbsent(callbackBatch.dependantChain(), k -> new LinkedHashMap<>())
            .putIfAbsent(dependencyId, callbackBatch)
        != null) {
      throw new DuplicateRequestException(
          "Duplicate data for dependency %s of kryon %s in dependant chain %s"
              .formatted(dependencyId, kryonId, callbackBatch.dependantChain()));
    }
  }
}
