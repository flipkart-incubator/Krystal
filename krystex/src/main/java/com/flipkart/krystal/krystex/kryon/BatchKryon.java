package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.flipkart.krystal.data.Errable.nil;
import static com.flipkart.krystal.data.Errable.withError;
import static com.flipkart.krystal.facets.resolution.ResolverCommand.executeWithRequests;
import static com.flipkart.krystal.facets.resolution.ResolverCommand.skip;
import static com.flipkart.krystal.krystex.kryon.KryonUtils.enqueueOrExecuteCommand;
import static com.google.common.base.Functions.identity;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.flipkart.krystal.data.DepResponse;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.data.FacetValuesContainer;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.except.SkippedExecutionException;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.FacetUtils;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.facets.resolution.ResolverCommand.ExecuteDependency;
import com.flipkart.krystal.facets.resolution.ResolverCommand.SkipDependency;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.CallbackCommand;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardReceive;
import com.flipkart.krystal.krystex.commands.ForwardSend;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.commands.MultiRequestCommand;
import com.flipkart.krystal.krystex.logicdecoration.FlushCommand;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import com.flipkart.krystal.krystex.resolution.Resolver;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
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
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
final class BatchKryon extends AbstractKryon<MultiRequestCommand, BatchResponse> {

  private final Map<DependantChain, Set<Facet>> availableFacetsByDepChain = new LinkedHashMap<>();

  private final Map<DependantChain, Map<RequestId, FacetValuesBuilder>> facetsCollector =
      new LinkedHashMap<>();

  private final Map<DependantChain, ForwardReceive> inputsValueCollector = new LinkedHashMap<>();

  /** A unique Result future for every dependant chain. */
  private final Map<DependantChain, CompletableFuture<BatchResponse>> resultsByDepChain =
      new LinkedHashMap<>();

  private final Map<DependantChain, Set<Facet>> executedDependencies = new LinkedHashMap<>();

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
  public CompletableFuture<BatchResponse> executeCommand(MultiRequestCommand kryonCommand) {
    DependantChain dependantChain = kryonCommand.dependantChain();
    final CompletableFuture<BatchResponse> resultForDepChain =
        resultsByDepChain.computeIfAbsent(dependantChain, r -> new CompletableFuture<>());
    try {
      if (kryonCommand instanceof ForwardReceive forward) {
        if (log.isDebugEnabled()) {
          forward
              .executableRequests()
              .forEach(
                  (requestId, facets) -> {
                    log.debug(
                        "Exec Ids - {}: {} invoked with inputs {}, in call path {}",
                        requestId,
                        kryonId,
                        facets,
                        forward.dependantChain());
                  });
        }
        collectInputValues(forward);
      } else if (kryonCommand instanceof CallbackCommand callbackBatch) {
        if (log.isDebugEnabled()) {
          callbackBatch
              .resultsByRequest()
              .forEach(
                  (requestId, results) -> {
                    log.debug(
                        "Exec Ids - {}: {} received response for dependency {} in call path {}. Response: {}",
                        requestId,
                        kryonId,
                        callbackBatch.dependency(),
                        callbackBatch.dependantChain(),
                        results);
                  });
        }
        collectDependencyValues(callbackBatch);
      }
      triggerDependencies(
          dependantChain,
          getTriggerableDependencies(dependantChain, facetsOfCommand(kryonCommand)));

      Optional<CompletableFuture<BatchResponse>> outputLogicFuture =
          executeOutputLogicIfPossible(dependantChain);
      outputLogicFuture.ifPresent(f -> linkFutures(f, resultForDepChain));
    } catch (Throwable e) {
      resultForDepChain.completeExceptionally(e);
    }
    return resultForDepChain;
  }

  private Map<Dependency, ImmutableSet<ResolverDefinition>> getTriggerableDependencies(
      DependantChain dependantChain, Set<? extends Facet> newFacets) {
    Set<Facet> availableInputs = availableFacetsByDepChain.getOrDefault(dependantChain, Set.of());
    Set<Facet> executedDeps = executedDependencies.getOrDefault(dependantChain, Set.of());

    return Stream.concat(
            Stream.concat(Stream.of(Optional.<Facet>empty()), newFacets.stream().map(Optional::of))
                .map(
                    key ->
                        kryonDefinition
                            .resolverDefinitionsByInput()
                            .getOrDefault(key, ImmutableSet.of()))
                .flatMap(Collection::stream)
                .map(resolver -> resolver.definition().target().dependency()),
            kryonDefinition.dependenciesWithNoResolvers().stream())
        .distinct()
        .filter(depName -> !executedDeps.contains(depName))
        .filter(
            depName ->
                kryonDefinition
                    .resolverDefinitionsByDependencies()
                    .getOrDefault(depName, ImmutableSet.of())
                    .stream()
                    .map(resolver -> resolver.definition().sources())
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
                        .map(resolver -> resolver.definition())
                        .collect(toImmutableSet())));
  }

  private void triggerDependencies(
      DependantChain dependantChain,
      Map<Dependency, ImmutableSet<ResolverDefinition>> triggerableDependencies) {
    ForwardReceive forwardBatch = getForwardCommand(dependantChain);
    if (log.isDebugEnabled()) {
      log.debug(
          "Exec ids: {}. Computed triggerable dependencies: {} of {} in call path {}",
          forwardBatch.requestIds(),
          triggerableDependencies.keySet(),
          kryonId,
          forwardBatch.dependantChain());
    }
    ImmutableMap<RequestId, String> skippedRequests = forwardBatch.skippedRequests();
    ImmutableSet<RequestId> executableRequests = forwardBatch.executableRequests().keySet();
    Map<Dependency, Map<Set<RequestId>, ResolverCommand>> commandsByDependency =
        new LinkedHashMap<>();
    if (!skippedRequests.isEmpty()) {
      SkipDependency skip = skip(String.join(", ", skippedRequests.values()));
      for (Dependency depName : triggerableDependencies.keySet()) {
        commandsByDependency
            .computeIfAbsent(depName, _k -> new LinkedHashMap<>())
            .put(skippedRequests.keySet(), skip);
      }
    }

    Set<Dependency> dependenciesWithNoResolvers =
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
                .put(Set.of(requestId), executeWithRequests(ImmutableList.of(emptyRequest())));
          });
      FacetValues facetValues = getFacetsFor(dependantChain, requestId);
      triggerableDependencies.forEach(
          (dep, resolverDefs) -> {
            List<ResolverDefinition> fanoutResolvers =
                resolverDefs.stream().filter(ResolverDefinition::canFanout).toList();
            List<ResolverDefinition> oneToOneResolvers =
                resolverDefs.stream()
                    .filter(resolverDefinition -> !resolverDefinition.canFanout())
                    .toList();
            ResolverDefinition fanoutResolverDef = null;
            if (fanoutResolvers.size() > 1) {
              throw new IllegalStateException(
                  "Multiple fanout resolvers found for dependency %s of vajram %s. This is not supported."
                      .formatted(dep, kryonId.value()));
            } else if (fanoutResolvers.size() == 1) {
              fanoutResolverDef = fanoutResolvers.get(0);
            }
            Supplier<Builder> newDepRequestBuilder =
                () ->
                    requireNonNull(
                            kryonDefinition
                                .kryonDefinitionRegistry()
                                .get(checkNotNull(kryonDefinition.dependencyKryons().get(dep))))
                        .createNewRequest()
                        .logic()
                        .newRequestBuilder();
            ImmutableList<? extends Builder> depRequestBuilders =
                ImmutableList.of(newDepRequestBuilder.get());
            ResolverCommand resolverCommand = null;
            for (ResolverDefinition resolverDef : oneToOneResolvers) {
              Resolver resolver = kryonDefinition.resolversByDefinition().get(resolverDef);
              if (resolver == null) {
                continue;
              }
              resolverCommand =
                  kryonDefinition
                      .kryonDefinitionRegistry()
                      .logicDefinitionRegistry()
                      .getResolver(resolver.resolverKryonLogicId())
                      .logic()
                      .resolve(depRequestBuilders, facetValues);
              if (resolverCommand instanceof ExecuteDependency
                  && resolverCommand.getRequests().isEmpty()) {
                // This should not happen. But if it does, we ignore this resolver invocation and
                // continue with the rest.
                continue;
              }
              if (resolverCommand instanceof SkipDependency) {
                break;
              }
              depRequestBuilders = resolverCommand.getRequests();
            }
            if (fanoutResolverDef != null && !(resolverCommand instanceof SkipDependency)) {
              Resolver fanoutResolver =
                  kryonDefinition.resolversByDefinition().get(fanoutResolverDef);
              if (fanoutResolver != null) {
                resolverCommand =
                    kryonDefinition
                        .kryonDefinitionRegistry()
                        .logicDefinitionRegistry()
                        .getResolver(fanoutResolver.resolverKryonLogicId())
                        .logic()
                        .resolve(depRequestBuilders, facetValues);
                if (resolverCommand instanceof ExecuteDependency
                    && resolverCommand.getRequests().isEmpty()) {
                  // This means the resolvers resolved any input. This can occur, for
                  // example if a fanout resolver returns empty inputs
                  resolverCommand = executeWithRequests(depRequestBuilders);
                }
              }
            }
            if (resolverCommand == null) {
              // This means the dependency has no resolvers. So continue to execute the dependency
              // with an empty request. This case can occur, for example, when all the inputs of
              // vajram are optional and the client vajram chooses not to write any resolvers for
              // the inputs, instead opting to go with the null values.
              resolverCommand = executeWithRequests(ImmutableList.of(newDepRequestBuilder.get()));
            }
            commandsByDependency
                .computeIfAbsent(dep, _k -> new LinkedHashMap<>())
                .put(Set.of(requestId), resolverCommand);
          });
    }
    for (var entry : commandsByDependency.entrySet()) {
      Dependency depId = entry.getKey();
      var resolverCommandsForDep = entry.getValue();
      triggerDependency(depId, dependantChain, resolverCommandsForDep);
    }
  }

  private FacetValuesBuilder facetsFromRequest(Request req) {
    return kryonDefinition.facetsFromRequest().logic().facetsFromRequest(req);
  }

  private FacetValuesBuilder emptyFacets() {
    return kryonDefinition.facetsFromRequest().logic().facetsFromRequest(emptyRequest());
  }

  private Builder<Object> emptyRequest() {
    //noinspection unchecked
    return (Builder) kryonDefinition.createNewRequest().logic().newRequestBuilder();
  }

  private ForwardReceive getForwardCommand(DependantChain dependantChain) {
    ForwardReceive forwardBatch = inputsValueCollector.get(dependantChain);
    if (forwardBatch == null) {
      throw new IllegalArgumentException("Missing Forward command. This should not be possible.");
    }
    return forwardBatch;
  }

  private void triggerDependency(
      Dependency dependency,
      DependantChain dependantChain,
      Map<Set<RequestId>, ResolverCommand> resolverCommandsByReq) {
    if (executedDependencies.getOrDefault(dependantChain, Set.of()).contains(dependency)) {
      return;
    }
    KryonId depKryonId = kryonDefinition.dependencyKryons().get(dependency);
    if (depKryonId == null) {
      throw new AssertionError(
          """
          Could not find kryon mapped to dependency name %s in kryon %s.
          This should not happen and is mostly a bug in the framework.
          """
              .formatted(dependency, kryonId));
    }
    Map<RequestId, ImmutableRequest> depRequestsByDepReqId = new LinkedHashMap<>();
    Map<RequestId, String> skipReasonsByReq = new LinkedHashMap<>();
    Map<RequestId, Set<RequestId>> depReqsByIncomingReq = new LinkedHashMap<>();
    for (var entry : resolverCommandsByReq.entrySet()) {
      Set<RequestId> incomingReqIds = entry.getKey();
      ResolverCommand resolverCommand = entry.getValue();
      if (resolverCommand instanceof SkipDependency skipDependency) {
        RequestId depReqId =
            requestIdGenerator.newSubRequest(
                incomingReqIds.iterator().next(), () -> "%s[skip]".formatted(dependency));
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
                requestIdGenerator.newSubRequest(
                    incomingReqId, () -> "%s[skip]".formatted(dependency));
            skipReasonsByReq.put(
                depReqId,
                "Resolvers for dependency %s resolved to empty list".formatted(dependency));
          } else {
            for (Request request : resolverCommand.getRequests()) {
              int currentCount = count++;
              RequestId depReqId =
                  requestIdGenerator.newSubRequest(
                      incomingReqId, () -> "%s[%s]".formatted(dependency, currentCount));
              depReqsByIncomingReq
                  .computeIfAbsent(incomingReqId, _k -> new LinkedHashSet<>())
                  .add(depReqId);
              depRequestsByDepReqId.put(depReqId, request._build());
            }
          }
        }
      }
    }
    executedDependencies
        .computeIfAbsent(dependantChain, _k -> new LinkedHashSet<>())
        .add(dependency);
    if (log.isDebugEnabled()) {
      skipReasonsByReq.forEach(
          (execId, reason) -> {
            log.debug(
                "Exec Ids: {}. Dependency {} of {} will be skipped due to reason {}",
                execId,
                Optional.ofNullable(kryonDefinition.dependencyKryons().get(dependency)),
                kryonId,
                reason);
          });
    }
    CompletableFuture<BatchResponse> depResponse =
        kryonExecutor.executeCommand(
            new ForwardSend(
                depKryonId,
                ImmutableMap.copyOf(depRequestsByDepReqId),
                dependantChain.extend(kryonId, dependency),
                ImmutableMap.copyOf(skipReasonsByReq)));

    depResponse.whenComplete(
        (batchResponse, throwable) -> {
          Set<RequestId> requestIds =
              resolverCommandsByReq.keySet().stream().flatMap(Collection::stream).collect(toSet());

          ImmutableMap<RequestId, DepResponse> results =
              requestIds.stream()
                  .collect(
                      toImmutableMap(
                          identity(),
                          requestId -> {
                            if (throwable != null) {
                              RequestResponse fail =
                                  new RequestResponse(emptyRequest(), withError(throwable));
                              if (dependency.canFanout()) {
                                return new FanoutDepResponses(ImmutableList.of(fail));
                              } else {
                                return fail;
                              }
                            } else {
                              Set<RequestId> depReqIds =
                                  depReqsByIncomingReq.getOrDefault(requestId, Set.of());
                              ImmutableList<RequestResponse> collect =
                                  depReqIds.stream()
                                      .map(
                                          depReqId -> {
                                            return new RequestResponse(
                                                (Request)
                                                    depRequestsByDepReqId.getOrDefault(
                                                        depReqId, emptyRequest()._build()),
                                                batchResponse
                                                    .responses()
                                                    .getOrDefault(depReqId, nil()));
                                          })
                                      .collect(toImmutableList());
                              if (dependency.canFanout()) {
                                return new FanoutDepResponses(collect);
                              } else if (collect.size() == 1) {
                                return collect.get(0);
                              } else {
                                throw new AssertionError(
                                    "Expected exactly one response for one2one dependency %s, but got %s"
                                        .formatted(dependency, collect));
                              }
                            }
                          }));

          enqueueOrExecuteCommand(
              () -> new CallbackCommand(kryonId, dependency, results, dependantChain),
              depKryonId,
              kryonDefinition,
              kryonExecutor);
        });
    flushDependencyIfNeeded(dependency, dependantChain);
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
                        Optional.ofNullable(kryonDefinition.dependencyKryons().get(dependency)),
                        depKryonId,
                        dependantChain,
                        timeout,
                        SECONDS);
                  }
                });
      }
    flushDependencyIfNeeded(dependency, dependantChain);
  }

  private Optional<CompletableFuture<BatchResponse>> executeOutputLogicIfPossible(
      DependantChain dependantChain) {

    if (outputLogicExecuted.getOrDefault(dependantChain, false)) {
      // Output logic aleady executed
      return Optional.empty();
    }

    ForwardReceive forwardCommand = getForwardCommand(dependantChain);
    // If all the inputs and dependency values needed by the output logic are available, then
    // prepare to run outputLogic
    ImmutableSet<Facet> facetIds = kryonDefinition.getOutputLogicDefinition().usedFacets();
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
    OutputLogic<Object> logic = outputLogicDefinition.logic()::execute;

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
                    .execute(ImmutableList.of(outputLogicFacets.allFacetValues()))
                    .values()
                    .iterator()
                    .next();
          } catch (Throwable e) {
            result = failedFuture(e);
          }
          resultsByRequest.put(
              requestId, result.<Errable<@NonNull Object>>handle(Errable::errableFrom));
        });
    return resultsByRequest;
  }

  private void flushAllDependenciesIfNeeded(DependantChain dependantChain) {
    kryonDefinition
        .dependencyKryons()
        .keySet()
        .forEach(dependencyName -> flushDependencyIfNeeded(dependencyName, dependantChain));
  }

  private void flushDependencyIfNeeded(Dependency dependencyId, DependantChain dependantChain) {
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

  private FacetValues getFacetsFor(DependantChain dependantChain, RequestId requestId) {
    return facetsCollector
        .getOrDefault(dependantChain, Map.of())
        .getOrDefault(requestId, emptyFacets());
  }

  private OutputLogicFacets getFacetsForOutputLogic(
      DependantChain dependantChain, RequestId requestId) {
    return new OutputLogicFacets(
        facetsCollector
            .getOrDefault(dependantChain, Map.of())
            .getOrDefault(
                requestId,
                facetsBuilderFromContainer(
                    getForwardCommand(dependantChain).executableRequests().get(requestId))));
  }

  private FacetValuesBuilder facetsBuilderFromContainer(
      @Nullable FacetValuesContainer facetValuesContainer) {
    if (facetValuesContainer == null) {
      return emptyFacets();
    } else if (facetValuesContainer instanceof Request request) {
      return facetsFromRequest(request);
    } else if (facetValuesContainer instanceof FacetValues facetValues) {
      return facetValues._asBuilder();
    } else {
      throw new UnsupportedOperationException(
          "Unknown container type " + facetValuesContainer.getClass());
    }
  }

  private void collectInputValues(ForwardReceive forwardBatch) {
    if (requestsByDependantChain.putIfAbsent(
            forwardBatch.dependantChain(), forwardBatch.requestIds())
        != null) {
      throw new DuplicateRequestException(
          "Duplicate batch request received for dependant chain %s"
              .formatted(forwardBatch.dependantChain()));
    }
    if (inputsValueCollector.putIfAbsent(forwardBatch.dependantChain(), forwardBatch) != null) {
      throw new DuplicateRequestException(
          "Duplicate ForwardBatch %s received for kryon %s in dependant chain %s"
              // TODO: Use input names instead of input ids
              .formatted(
                  inputsValueCollector.get(forwardBatch.dependantChain()),
                  kryonId,
                  forwardBatch.dependantChain()));
    }
    availableFacetsByDepChain
        .computeIfAbsent(forwardBatch.dependantChain(), _k -> new LinkedHashSet<>())
        .addAll(facetsOfCommand(forwardBatch));

    forwardBatch
        .executableRequests()
        .forEach(
            (requestId, container) -> {
              facetsCollector
                  .computeIfAbsent(forwardBatch.dependantChain(), _d -> new LinkedHashMap<>())
                  .put(requestId, facetsBuilderFromContainer(container));
            });
  }

  private Set<? extends Facet> facetsOfCommand(KryonCommand command) {
    if (command instanceof CallbackCommand callbackBatch) {
      return callbackBatch.facets();
    } else if (command instanceof ForwardReceive) {
      return kryonDefinition.facets().stream().filter(FacetUtils::isGiven).collect(toSet());
    } else {
      throw new UnsupportedOperationException("" + command);
    }
  }

  private static String getSkipMessage(ForwardReceive forwardBatch) {
    return String.join(", ", forwardBatch.skippedRequests().values());
  }

  private void collectDependencyValues(CallbackCommand callbackBatch) {
    Dependency dependencyId = callbackBatch.dependency();
    availableFacetsByDepChain
        .computeIfAbsent(callbackBatch.dependantChain(), _k -> new LinkedHashSet<>())
        .add(dependencyId);
    //    if (dependencyValuesCollector
    //            .computeIfAbsent(callbackBatch.dependantChain(), k -> new LinkedHashMap<>())
    //            .putIfAbsent(dependencyId, callbackBatch)
    //        != null) {
    //      throw new DuplicateRequestException(
    //          "Duplicate data for dependency %s of kryon %s in dependant chain %s"
    //              .formatted(dependencyId, kryonId, callbackBatch.dependantChain()));
    //    }
    callbackBatch
        .resultsByRequest()
        .forEach(
            (requestId, depResponse) -> {
              FacetValuesBuilder facetsBuilder =
                  facetsCollector
                      .computeIfAbsent(callbackBatch.dependantChain(), _d -> new LinkedHashMap<>())
                      .get(requestId);
              if (facetsBuilder == null) {
                // This means this request was skipped. Hence no facet builder is present for this
                // request.
                return;
              }
              callbackBatch.dependency().setFacetValue(facetsBuilder, depResponse);
            });
  }
}
