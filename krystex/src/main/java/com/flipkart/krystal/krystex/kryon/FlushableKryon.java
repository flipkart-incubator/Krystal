package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.flipkart.krystal.data.Errable.nil;
import static com.flipkart.krystal.data.Errable.withError;
import static com.flipkart.krystal.except.StackTracelessException.stackTracelessWrap;
import static com.flipkart.krystal.facets.resolution.ResolverCommand.executeWithRequests;
import static com.flipkart.krystal.facets.resolution.ResolverCommand.skip;
import static com.flipkart.krystal.krystex.kryon.KryonUtils.enqueueOrExecuteCommand;
import static com.google.common.base.Functions.identity;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.flipkart.krystal.core.OutputLogicExecutionInput;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.DepResponse;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.data.FacetValuesContainer;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponse;
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
import com.flipkart.krystal.krystex.commands.VoidResponse;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.dependencydecoration.VajramInvocation;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import com.flipkart.krystal.krystex.resolution.Resolver;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A flushable kryon works in a single request-single response mode. A given kryon receives one or
 * more requests in a single {@link ForwardSend} command from a client with a given {@link
 * DependentChain dependent chain}. A given client kryon cannot send multiple {@link ForwardSend}
 * commands to another kryon in the same dependent chain. This way a Flushable Kryon is able to keep
 * track of incoming requests per dependent chain and thus is able to send a {@link Flush} command
 * to its dependencies per dependent chain. This "flushing" capability is crucial for achieving
 * capabilities like optimal batching (For example:
 * com.flipkart.krystal.vajramexecutor.krystex.InputBatchingDecorator) etc which rely on the fact
 * that they are able to track the complete super set of active dependent chains and able to
 * determine accurately when the call graph execution has reached a point where not further requests
 * can be received.
 *
 * <p>This design choice of singleRequest-SingleResponse, as well as flushing means has the
 * following implication on this class' design:
 *
 * <ul>
 *   <li>The class using {@link CompletableFuture}s for tracking responses
 *   <li>This class performs book keeping of what commands were received and what commands were sent
 *       in in-memory data structures which are not cleared up because the complete executor and all
 *       kryons will anyway be garbage collected once the {@link KryonExecutor} is closed. This is
 *       acceptable since each executor is expected to last for a few seconds, not more (this is the
 *       nature of the request-response paradigm)
 * </ul>
 */
@Slf4j
final class FlushableKryon extends AbstractKryon<MultiRequestCommand, BatchResponse> {

  private final Map<DependentChain, Set<Facet>> availableFacetsByDepChain = new LinkedHashMap<>();

  private final Map<DependentChain, Map<InvocationId, FacetValuesBuilder>> facetsCollector =
      new LinkedHashMap<>();

  private final Map<DependentChain, ForwardReceive> inputsValueCollector = new LinkedHashMap<>();

  /** A unique Result future for every dependant chain. */
  private final Map<DependentChain, CompletableFuture<BatchResponse>> resultsByDepChain =
      new LinkedHashMap<>();

  private final Map<DependentChain, Set<Facet>> executedDependencies = new LinkedHashMap<>();

  private final Map<DependentChain, Set<InvocationId>> invocationsByDependentChain =
      new LinkedHashMap<>();

  private final Set<DependentChain> flushedDependentChain = new LinkedHashSet<>();
  private final Map<DependentChain, Boolean> outputLogicExecuted = new LinkedHashMap<>();

  FlushableKryon(
      VajramKryonDefinition kryonDefinition,
      KryonExecutor kryonExecutor,
      Function<LogicExecutionContext, ImmutableMap<String, OutputLogicDecorator>>
          outputLogicDecoratorSuppliers,
      Function<DependencyExecutionContext, ImmutableMap<String, DependencyDecorator>>
          depDecoratorSuppliers,
      DecorationOrdering decorationOrdering,
      RequestIdGenerator requestIdGenerator) {
    super(
        kryonDefinition,
        kryonExecutor,
        outputLogicDecoratorSuppliers,
        depDecoratorSuppliers,
        decorationOrdering,
        requestIdGenerator);
  }

  @Override
  public void executeCommand(Flush flushCommand) {
    flushedDependentChain.add(flushCommand.dependentChain());
    flushAllDependenciesIfNeeded(flushCommand.dependentChain());
    flushDecoratorsIfNeeded(flushCommand.dependentChain());
  }

  @Override
  public CompletableFuture<BatchResponse> executeCommand(MultiRequestCommand kryonCommand) {
    DependentChain dependentChain = kryonCommand.dependentChain();
    final CompletableFuture<BatchResponse> resultForDepChain =
        resultsByDepChain.computeIfAbsent(dependentChain, r -> new CompletableFuture<>());
    try {
      if (kryonCommand instanceof ForwardReceive forward) {
        if (log.isDebugEnabled()) {
          forward
              .executableInvocations()
              .forEach(
                  (requestId, facets) ->
                      log.debug(
                          "Exec Ids - {}: {} invoked with inputs {}, in call path {}",
                          requestId,
                          vajramID,
                          facets,
                          forward.dependentChain()));
        }
        collectInputValues(forward);
      } else if (kryonCommand instanceof CallbackCommand callbackBatch) {
        if (log.isDebugEnabled()) {
          callbackBatch
              .resultsByRequest()
              .forEach(
                  (requestId, results) ->
                      log.debug(
                          "Exec Ids - {}: {} received response for dependency {} in call path {}. Response: {}",
                          requestId,
                          vajramID,
                          callbackBatch.dependency(),
                          callbackBatch.dependentChain(),
                          results));
        }
        collectDependencyValues(callbackBatch);
      }
      triggerDependencies(
          dependentChain,
          getTriggerableDependencies(dependentChain, facetsOfCommand(kryonCommand)));

      Optional<CompletableFuture<BatchResponse>> outputLogicFuture =
          executeOutputLogicIfPossible(dependentChain);
      outputLogicFuture.ifPresent(f -> linkFutures(f, resultForDepChain));
    } catch (Throwable e) {
      resultForDepChain.completeExceptionally(stackTracelessWrap(e));
    }
    return resultForDepChain;
  }

  private Map<Dependency, ImmutableSet<Resolver>> getTriggerableDependencies(
      DependentChain dependentChain, Set<? extends Facet> newFacets) {
    Set<Facet> availableFacets = availableFacetsByDepChain.getOrDefault(dependentChain, Set.of());
    Set<Facet> executedDeps = executedDependencies.getOrDefault(dependentChain, Set.of());
    Set<Dependency> dependenciesForNewFacetNames = new LinkedHashSet<>();
    for (Facet newFacetName : newFacets) {
      dependenciesForNewFacetNames.addAll(
          kryonDefinition.dependenciesByBoundFacet().getOrDefault(newFacetName, ImmutableSet.of()));
    }
    Map<Dependency, ImmutableSet<Resolver>> triggerableDependencies = new LinkedHashMap<>();
    for (Dependency depName :
        Iterables.concat(
            dependenciesForNewFacetNames,
            kryonDefinition.dependenciesWithNoFacetResolvers(),
            kryonDefinition.dependenciesWithNoResolvers())) {
      if (executedDeps.contains(depName)) {
        continue;
      }
      if (availableFacets.containsAll(
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
      DependentChain dependentChain,
      Map<Dependency, ImmutableSet<Resolver>> triggerableDependencies) {
    ForwardReceive forwardBatch = getForwardCommand(dependentChain);
    if (log.isDebugEnabled()) {
      log.debug(
          "Exec ids: {}. Computed triggerable dependencies: {} of {} in call path {}",
          forwardBatch.invocationIds(),
          triggerableDependencies.keySet(),
          vajramID,
          forwardBatch.dependentChain());
    }
    Map<InvocationId, String> skippedRequests = forwardBatch.invocationsToSkip();
    Set<InvocationId> executableRequests = forwardBatch.executableInvocations().keySet();
    Map<Dependency, Map<Set<InvocationId>, ResolverCommand>> commandsByDependency =
        new LinkedHashMap<>();
    Map<Dependency, Set<InvocationId>> requestIdsByDependency = new LinkedHashMap<>();
    if (!skippedRequests.isEmpty()) {
      SkipDependency skip = skip(String.join(", ", skippedRequests.values()));
      for (Dependency depName : triggerableDependencies.keySet()) {
        commandsByDependency
            .computeIfAbsent(depName, _k -> new LinkedHashMap<>())
            .put(unmodifiableSet(skippedRequests.keySet()), skip);
        requestIdsByDependency
            .computeIfAbsent(depName, _k -> new LinkedHashSet<>())
            .addAll(skippedRequests.keySet());
      }
    }

    SetView<Dependency> triggerablesWithNoResolvers =
        Sets.intersection(
            kryonDefinition.dependenciesWithNoResolvers(), triggerableDependencies.keySet());
    for (InvocationId invocationId : executableRequests) {
      triggerablesWithNoResolvers.forEach(
          depName -> {
            // For such dependencies, trigger them with empty inputs
            commandsByDependency
                .computeIfAbsent(depName, _k -> new LinkedHashMap<>())
                .put(Set.of(invocationId), executeWithRequests(ImmutableList.of(emptyRequest())));
            requestIdsByDependency
                .computeIfAbsent(depName, _k -> new LinkedHashSet<>())
                .add(invocationId);
          });
      FacetValues facetValues = getFacetsFor(dependentChain, invocationId);
      triggerableDependencies.forEach(
          (dep, resolverDefs) -> {
            VajramID depVajramId = kryonDefinition.dependencyKryons().get(dep);
            KryonDefinition depKryonDefinition =
                kryonDefinition.kryonDefinitionRegistry().get(checkNotNull(depVajramId));
            if (depKryonDefinition == null) {
              commandsByDependency
                  .computeIfAbsent(dep, _k -> new LinkedHashMap<>())
                  .put(
                      Set.of(invocationId),
                      skip("Could not find dependency with vajram ID " + depVajramId));
              return;
            }
            Resolver fanoutResolver = null;
            List<Resolver> oneToOneResolvers = new ArrayList<>(resolverDefs.size());
            for (Resolver resolver : resolverDefs) {
              if (resolver.definition().canFanout()) {
                if (fanoutResolver != null) {
                  throw new IllegalStateException(
                      "Multiple fanout resolvers found for dependency %s of vajram %s. This is not supported."
                          .formatted(dep, vajramID.id()));
                }
                fanoutResolver = resolver;
              } else {
                oneToOneResolvers.add(resolver);
              }
            }
            Supplier<ImmutableRequest.Builder<?>> newDepRequestBuilder =
                () -> depKryonDefinition.createNewRequest().logic().newRequestBuilder();
            ImmutableList<? extends ImmutableRequest.Builder<?>> depRequestBuilders =
                ImmutableList.of(newDepRequestBuilder.get());
            ResolverCommand resolverCommand = null;
            for (Resolver resolver : oneToOneResolvers) {
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
            if (fanoutResolver != null && !(resolverCommand instanceof SkipDependency)) {
              resolverCommand =
                  kryonDefinition
                      .kryonDefinitionRegistry()
                      .logicDefinitionRegistry()
                      .getResolver(fanoutResolver.resolverKryonLogicId())
                      .logic()
                      .resolve(depRequestBuilders, facetValues);
              if (resolverCommand instanceof ExecuteDependency
                  && resolverCommand.getRequests().isEmpty()) {
                // This means the resolvers did not resolve any input. This can occur, for
                // example if a fanout resolver returns empty inputs. When a fanout resolver
                // returns empty results, we continue to execute the dependency as if the fanout
                // resolver returned one null value. This is done so that developers don't
                // accidentally end up skipping a dependency by resolving an empty list. We
                // interpret such a resolution as the following developer intent: "I want to
                // execute the dependency, but I do not know what value to resolve - so execute
                // with some default value"
                resolverCommand = executeWithRequests(depRequestBuilders);
              }
            }
            if (resolverCommand == null) {
              // This means the dependency has no resolvers. So continue to execute the dependency
              // with an empty request. This case can occur, for example, when all the inputs of
              // vajram are optional and the client vajram chooses not to write any resolvers for
              // the inputs, instead opting to go with the null values.
              resolverCommand = executeWithRequests(depRequestBuilders);
            }
            commandsByDependency
                .computeIfAbsent(dep, _k -> new LinkedHashMap<>())
                .put(Set.of(invocationId), resolverCommand);
            requestIdsByDependency
                .computeIfAbsent(dep, _k -> new LinkedHashSet<>())
                .add(invocationId);
          });
    }
    for (var entry : commandsByDependency.entrySet()) {
      Dependency dependency = entry.getKey();
      var resolverCommandsForDep = entry.getValue();
      triggerDependency(
          dependency,
          dependentChain,
          resolverCommandsForDep,
          requestIdsByDependency.getOrDefault(dependency, Set.of()));
    }
  }

  private FacetValuesBuilder emptyFacets() {
    return kryonDefinition.facetsFromRequest().logic().facetsFromRequest(emptyRequest());
  }

  @SuppressWarnings("unchecked")
  private ImmutableRequest.Builder<@Nullable Object> emptyRequest() {
    return (ImmutableRequest.Builder<@Nullable Object>)
        kryonDefinition.createNewRequest().logic().newRequestBuilder();
  }

  @SuppressWarnings("unchecked")
  private ImmutableRequest.Builder<@Nullable Object> emptyRequestForVajram(VajramID depVajramID) {
    return (ImmutableRequest.Builder<@Nullable Object>)
        kryonDefinition
            .kryonDefinitionRegistry()
            .getOrThrow(depVajramID)
            .createNewRequest()
            .logic()
            .newRequestBuilder();
  }

  private ForwardReceive getForwardCommand(DependentChain dependentChain) {
    ForwardReceive forwardBatch = inputsValueCollector.get(dependentChain);
    if (forwardBatch == null) {
      throw new IllegalArgumentException("Missing Forward command. This should not be possible.");
    }
    return forwardBatch;
  }

  @SuppressWarnings({"FutureReturnValueIgnored", "unchecked"})
  private void triggerDependency(
      Dependency dependency,
      DependentChain dependentChain,
      Map<Set<InvocationId>, ResolverCommand> resolverCommandsByReq,
      Set<InvocationId> allInvocationIds) {
    if (executedDependencies.getOrDefault(dependentChain, Set.of()).contains(dependency)) {
      return;
    }
    VajramID depVajramID = kryonDefinition.dependencyKryons().get(dependency);
    if (depVajramID == null) {
      throw new AssertionError(
          """
          Could not find kryon mapped to dependency name %s in kryon %s.
          This should not happen and is mostly a bug in the framework.
          """
              .formatted(dependency, vajramID));
    }
    Map<InvocationId, Request<@Nullable Object>> depRequestsByDepInvocId = new LinkedHashMap<>();
    Map<InvocationId, String> skipReasonsByReq = new LinkedHashMap<>();
    Map<InvocationId, Set<InvocationId>> depReqsByIncomingReq = new LinkedHashMap<>();
    for (var entry : resolverCommandsByReq.entrySet()) {
      Set<InvocationId> incomingReqIds = entry.getKey();
      ResolverCommand resolverCommand = entry.getValue();
      if (resolverCommand instanceof SkipDependency skipDependency) {
        InvocationId depReqId =
            requestIdGenerator.newSubRequest(
                incomingReqIds.iterator().next(), () -> "%s[skip]".formatted(dependency));
        skipReasonsByReq.put(depReqId, skipDependency.reason());
      } else {
        int count = 0;
        for (InvocationId incomingReqId : incomingReqIds) {
          if (resolverCommand.getRequests().isEmpty()) {
            InvocationId depReqId =
                requestIdGenerator.newSubRequest(
                    incomingReqId, () -> "%s[skip]".formatted(dependency));
            skipReasonsByReq.put(
                depReqId,
                "Resolvers for dependency %s resolved to empty list".formatted(dependency));
          } else {
            for (Request request : resolverCommand.getRequests()) {
              int currentCount = count++;
              InvocationId depReqId =
                  requestIdGenerator.newSubRequest(
                      incomingReqId, () -> "%s[%s]".formatted(dependency, currentCount));
              depReqsByIncomingReq
                  .computeIfAbsent(incomingReqId, _k -> new LinkedHashSet<>())
                  .add(depReqId);
              depRequestsByDepInvocId.put(depReqId, request._build());
            }
          }
        }
      }
    }
    executedDependencies
        .computeIfAbsent(dependentChain, _k -> new LinkedHashSet<>())
        .add(dependency);
    if (log.isDebugEnabled()) {
      skipReasonsByReq.forEach(
          (execId, reason) ->
              log.debug(
                  "Exec Ids: {}. Dependency {} of {} will be skipped due to reason {}",
                  execId,
                  Optional.ofNullable(kryonDefinition.dependencyKryons().get(dependency)),
                  vajramID,
                  reason));
    }
    DependentChain extendedDependentChain = dependentChain.extend(vajramID, dependency);

    VajramInvocation<BatchResponse> kryonResponseVajramInvocation =
        decorateVajramInvocation(
            extendedDependentChain, depVajramID, kryonExecutor::executeCommand);

    CompletableFuture<BatchResponse> depResponse =
        kryonResponseVajramInvocation.invokeDependency(
            new ForwardSend(
                depVajramID, depRequestsByDepInvocId, extendedDependentChain, skipReasonsByReq));

    depResponse.whenComplete(
        (batchResponse, throwable) -> {
          Map<InvocationId, DepResponse<Request<@Nullable Object>, @Nullable Object>> results =
              new LinkedHashMap<>();
          for (InvocationId invocationId : allInvocationIds) {
            DepResponse<Request<@Nullable Object>, @Nullable Object> result;
            if (throwable != null) {
              RequestResponse<Request<@Nullable Object>, @Nullable Object> fail =
                  new RequestResponse<>(
                      depRequestsByDepInvocId.getOrDefault(
                          invocationId, emptyRequestForVajram(depVajramID)._build()),
                      withError(throwable));
              if (dependency.canFanout()) {
                result = new FanoutDepResponses<>(ImmutableList.of(fail));
              } else {
                result = fail;
              }
            } else {
              Set<InvocationId> depReqIds =
                  depReqsByIncomingReq.getOrDefault(invocationId, Set.of());
              List<RequestResponse<Request<@Nullable Object>, @Nullable Object>> collect =
                  new ArrayList<>(depReqIds.size());
              for (InvocationId depReqId : depReqIds) {
                collect.add(
                    new RequestResponse<>(
                        depRequestsByDepInvocId.getOrDefault(
                            depReqId, emptyRequestForVajram(depVajramID)._build()),
                        batchResponse.responses().getOrDefault(depReqId, nil())));
              }

              if (dependency.canFanout()) {
                result = new FanoutDepResponses<>(collect);
              } else if (collect.size() == 1) {
                result = collect.get(0);
              } else {
                // This means this non-fanout dependency was skipped
                result = One2OneDepResponse.noRequest();
              }
            }
            results.put(invocationId, result);
          }

          enqueueOrExecuteCommand(
              () -> new CallbackCommand(vajramID, dependency, results, dependentChain),
              kryonExecutor);
        });
    flushDependencyIfNeeded(dependency, depVajramID, dependentChain);
    if (log.isDebugEnabled()) {
      logWaitingMessage(dependency, dependentChain, depResponse, depVajramID);
    }
    flushDependencyIfNeeded(dependency, depVajramID, dependentChain);
  }

  private <R extends KryonCommandResponse> VajramInvocation<R> decorateVajramInvocation(
      DependentChain dependentChain,
      VajramID depVajramID,
      VajramInvocation<R> invocationToDecorate) {
    for (DependencyDecorator dependencyDecorator :
        getSortedDependencyDecorators(depVajramID, dependentChain)) {
      VajramInvocation<R> previousDecoratedInvocation = invocationToDecorate;
      invocationToDecorate = dependencyDecorator.decorateDependency(previousDecoratedInvocation);
    }
    return invocationToDecorate;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void logWaitingMessage(
      Dependency dependency,
      DependentChain dependentChain,
      CompletableFuture<BatchResponse> depResponse,
      VajramID depVajramID) {
    for (int timeout : List.of(5, 10, 15)) {
      depResponse
          .copy()
          .orTimeout(timeout, SECONDS)
          .whenComplete(
              (_r, throwable) -> {
                if (throwable instanceof TimeoutException) {
                  log.debug(
                      "KryonId: {}, Dependency: {} on: {} with depChain: {}. Status: Waiting since {} {}",
                      vajramID,
                      Optional.ofNullable(kryonDefinition.dependencyKryons().get(dependency)),
                      depVajramID,
                      dependentChain,
                      timeout,
                      SECONDS);
                }
              });
    }
  }

  private Optional<CompletableFuture<BatchResponse>> executeOutputLogicIfPossible(
      DependentChain dependentChain) {

    if (outputLogicExecuted.getOrDefault(dependentChain, false)) {
      // Output logic already executed
      return Optional.empty();
    }

    ForwardReceive forwardCommand = getForwardCommand(dependentChain);
    // If all the inputs and dependency values needed by the output logic are available, then
    // prepare to run outputLogic
    ImmutableSet<Facet> facetIds = kryonDefinition.getOutputLogicDefinition().usedFacets();
    if (availableFacetsByDepChain
        .getOrDefault(dependentChain, ImmutableSet.of())
        .containsAll(facetIds)) { // All the facets of the kryon logic have data present
      if (!forwardCommand.executableInvocations().isEmpty()) {
        return Optional.of(
            executeOutputLogic(forwardCommand.executableInvocations().keySet(), dependentChain));
      } else {
        return Optional.of(completedFuture(BatchResponse.empty()));
      }
    }
    return Optional.empty();
  }

  private CompletableFuture<BatchResponse> executeOutputLogic(
      Set<? extends InvocationId> invocationIds, DependentChain dependentChain) {

    OutputLogicDefinition<Object> outputLogicDefinition =
        kryonDefinition.getOutputLogicDefinition();

    Map<InvocationId, OutputLogicFacets> outputLogicInputs = new LinkedHashMap<>();

    for (InvocationId invocationId : invocationIds) {
      outputLogicInputs.put(invocationId, getFacetsForOutputLogic(dependentChain, invocationId));
    }
    CompletableFuture<BatchResponse> resultForBatch = new CompletableFuture<>();
    Map<InvocationId, CompletableFuture<Errable<@Nullable Object>>> resultFutures =
        executeDecoratedOutputLogic(outputLogicDefinition, outputLogicInputs, dependentChain);

    var ignored =
        allOf(resultFutures.values().toArray(CompletableFuture[]::new))
            .whenComplete(
                (unused, throwable) -> {
                  Map<InvocationId, Errable<@Nullable Object>> responses = new LinkedHashMap<>();
                  for (InvocationId invocationId : outputLogicInputs.keySet()) {
                    responses.put(
                        invocationId,
                        resultFutures
                            .getOrDefault(invocationId, new CompletableFuture<>())
                            .getNow(nil()));
                  }
                  resultForBatch.complete(new BatchResponse(responses));
                });
    outputLogicExecuted.put(dependentChain, true);
    flushDecoratorsIfNeeded(dependentChain);
    return resultForBatch;
  }

  private Map<InvocationId, CompletableFuture<Errable<@Nullable Object>>>
      executeDecoratedOutputLogic(
          OutputLogicDefinition<Object> outputLogicDefinition,
          Map<InvocationId, OutputLogicFacets> inputs,
          DependentChain dependentChain) {
    NavigableSet<OutputLogicDecorator> sortedDecorators =
        getSortedOutputLogicDecorators(dependentChain);
    OutputLogic<Object> logic = outputLogicDefinition.logic()::execute;

    for (OutputLogicDecorator outputLogicDecorator : sortedDecorators) {
      logic = outputLogicDecorator.decorateLogic(logic, outputLogicDefinition);
    }
    OutputLogic<Object> finalLogic = logic;
    Map<InvocationId, CompletableFuture<Errable<@Nullable Object>>> resultsByRequest =
        new LinkedHashMap<>();
    inputs.forEach(
        (invocationId, outputLogicFacets) -> {
          CompletableFuture<@Nullable Object> result;
          try {
            result =
                finalLogic
                    .execute(
                        new OutputLogicExecutionInput(
                            ImmutableList.of(outputLogicFacets.allFacetValues()),
                            kryonExecutor.commandQueue()))
                    .results()
                    .values()
                    .iterator()
                    .next();
          } catch (Throwable e) {
            result = failedFuture(e);
          }
          resultsByRequest.put(invocationId, result.handle(Errable::errableFrom));
        });
    return resultsByRequest;
  }

  private void flushAllDependenciesIfNeeded(DependentChain dependentChain) {
    kryonDefinition
        .dependencyKryons()
        .forEach((key, value) -> flushDependencyIfNeeded(key, value, dependentChain));
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void flushDependencyIfNeeded(
      Dependency dependency, VajramID depVajramID, DependentChain dependentChain) {
    if (!flushedDependentChain.contains(dependentChain)) {
      return;
    }
    if (executedDependencies.getOrDefault(dependentChain, Set.of()).contains(dependency)) {
      DependentChain extendDependentChain = dependentChain.extend(vajramID, dependency);
      VajramInvocation<VoidResponse> decoratedVajramInvocation =
          decorateVajramInvocation(
              extendDependentChain, depVajramID, kryonExecutor::executeCommand);
      decoratedVajramInvocation.invokeDependency(
          new Flush(
              checkNotNull(
                  kryonDefinition.dependencyKryons().get(dependency),
                  "Could not find KryonId for dependency " + dependency + ". This is a bug"),
              extendDependentChain));
    }
  }

  private void flushDecoratorsIfNeeded(DependentChain dependentChain) {
    if (!flushedDependentChain.contains(dependentChain)) {
      return;
    }
    if (outputLogicExecuted.getOrDefault(dependentChain, false)
        || getForwardCommand(dependentChain).shouldSkip()) {
      Iterable<OutputLogicDecorator> reverseSortedDecorators =
          getSortedOutputLogicDecorators(dependentChain)::descendingIterator;
      for (OutputLogicDecorator decorator : reverseSortedDecorators) {
        try {
          decorator.executeCommand(new FlushCommand(dependentChain));
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

  private FacetValues getFacetsFor(DependentChain dependentChain, InvocationId invocationId) {
    return facetsCollector
        .computeIfAbsent(dependentChain, _k -> new LinkedHashMap<>())
        .computeIfAbsent(invocationId, _k -> emptyFacets());
  }

  private OutputLogicFacets getFacetsForOutputLogic(
      DependentChain dependentChain, InvocationId invocationId) {
    return new OutputLogicFacets(
        facetsCollector
            .computeIfAbsent(dependentChain, _k -> new LinkedHashMap<>())
            .computeIfAbsent(
                invocationId,
                _k ->
                    getForwardCommand(dependentChain)
                        .executableInvocations()
                        .getOrDefault(invocationId, emptyFacets())
                        ._asBuilder()));
  }

  private void collectInputValues(ForwardReceive forwardBatch) {
    if (invocationsByDependentChain.putIfAbsent(
            forwardBatch.dependentChain(), forwardBatch.invocationIds())
        != null) {
      throw new DuplicateRequestException(
          "Duplicate batch request received for dependant chain %s"
              .formatted(forwardBatch.dependentChain()));
    }
    if (inputsValueCollector.putIfAbsent(forwardBatch.dependentChain(), forwardBatch) != null) {
      throw new DuplicateRequestException(
          "Duplicate ForwardBatch %s received for kryon %s in dependant chain %s"
              // TODO: Use input names instead of input ids
              .formatted(
                  inputsValueCollector.get(forwardBatch.dependentChain()),
                  vajramID,
                  forwardBatch.dependentChain()));
    }
    availableFacetsByDepChain
        .computeIfAbsent(forwardBatch.dependentChain(), _k -> new LinkedHashSet<>())
        .addAll(kryonDefinition.givenFacets());

    forwardBatch
        .executableInvocations()
        .forEach(
            (requestId, container) ->
                facetsCollector
                    .computeIfAbsent(forwardBatch.dependentChain(), _d -> new LinkedHashMap<>())
                    .put(requestId, container._asBuilder()));
  }

  private Set<? extends Facet> facetsOfCommand(KryonCommand command) {
    if (command instanceof CallbackCommand callbackBatch) {
      return Set.of(callbackBatch.dependency());
    } else if (command instanceof ForwardReceive) {
      return kryonDefinition.givenFacets();
    } else {
      throw new UnsupportedOperationException("" + command);
    }
  }

  private void collectDependencyValues(CallbackCommand callbackBatch) {
    Dependency dependencyId = callbackBatch.dependency();
    availableFacetsByDepChain
        .computeIfAbsent(callbackBatch.dependentChain(), _k -> new LinkedHashSet<>())
        .add(dependencyId);
    callbackBatch
        .resultsByRequest()
        .forEach(
            (requestId, depResponse) -> {
              FacetValuesBuilder facetsBuilder =
                  facetsCollector
                      .computeIfAbsent(callbackBatch.dependentChain(), _d -> new LinkedHashMap<>())
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
