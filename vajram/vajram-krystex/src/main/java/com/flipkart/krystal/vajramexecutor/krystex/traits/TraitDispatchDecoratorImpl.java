package com.flipkart.krystal.vajramexecutor.krystex.traits;

import static com.flipkart.krystal.traits.matchers.InputValueMatcher.isAnyValue;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.commands.ClientSideCommand;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardSend;
import com.flipkart.krystal.krystex.dependencydecoration.VajramInvocation;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.FlushResponse;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.traits.PredicateDynamicDispatchPolicy;
import com.flipkart.krystal.traits.PredicateDynamicDispatchPolicy.DispatchCase;
import com.flipkart.krystal.traits.StaticDispatchPolicy;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TraitDispatchDecoratorImpl implements TraitDispatchDecorator {

  public static final String DECORATOR_TYPE = StaticDispatchPolicy.class.getName();

  private final VajramKryonGraph vajramKryonGraph;
  @Getter private final ImmutableMap<VajramID, TraitDispatchPolicy> traitDispatchPolicies;

  public TraitDispatchDecoratorImpl(
      VajramKryonGraph vajramKryonGraph,
      ImmutableMap<VajramID, TraitDispatchPolicy> traitDispatchPolicies) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.traitDispatchPolicies = traitDispatchPolicies;
  }

  @Override
  public <R extends KryonResponse> VajramInvocation<R> decorateDependency(
      VajramInvocation<R> invocationToDecorate) {
    return kryonCommand -> {
      VajramID traitId = kryonCommand.vajramID();
      VajramDefinition vajramDefinition = vajramKryonGraph.getVajramDefinition(traitId);
      if (!vajramDefinition.isTrait()) {
        return invocationToDecorate.invokeDependency(kryonCommand);
      }
      TraitDispatchPolicy traitDispatchPolicy = traitDispatchPolicies.get(traitId);
      if (traitDispatchPolicy instanceof StaticDispatchPolicy staticDispatchDefinition) {
        VajramID boundVajram;
        ClientSideCommand<R> commandToDispatch;
        Dependency dependency = kryonCommand.dependantChain().latestDependency();
        if (dependency != null) {
          boundVajram = staticDispatchDefinition.get(dependency);
        } else {
          throw new AssertionError(
              "This is not possible. A dependency decorator can only be invoked when there is a depednency present.");
        }
        commandToDispatch = transformCommandForDispatch(kryonCommand, boundVajram);
        if (commandToDispatch == null) {
          commandToDispatch = kryonCommand;
        }
        return invocationToDecorate.invokeDependency(commandToDispatch);
      } else if (traitDispatchPolicy instanceof PredicateDynamicDispatchPolicy dynamicPolicy) {
        Set<? extends InputMirrorSpec<?, ?>> dispatchEnabledFacets =
            dynamicPolicy.facets().stream()
                .filter(facet -> facet instanceof InputMirrorSpec<?, ?>)
                .map(facet -> (InputMirrorSpec<?, ?>) facet)
                .collect(toSet());
        ImmutableList<DispatchCase> dispatchCases = dynamicPolicy.dispatchCases();
        if (kryonCommand instanceof ForwardSend forwardSend) {
          ImmutableMap<RequestId, ? extends Request<?>> executabledRequests =
              forwardSend.executableRequests();
          Map<VajramID, Map<RequestId, Request<?>>> dispatchRequests = new LinkedHashMap<>();
          Map<VajramID, CompletableFuture<BatchResponse>> dispatchResponses = new LinkedHashMap<>();
          Set<RequestId> orphanedRequests = new LinkedHashSet<>();
          for (Entry<RequestId, ? extends Request<?>> requestEntry :
              executabledRequests.entrySet()) {
            RequestId requestId = requestEntry.getKey();
            Request<?> originalRequest = requestEntry.getValue();
            boolean dispatchTargetNotFound = true;
            for (DispatchCase dispatchCase : dispatchCases) {
              boolean caseMatches = true;
              for (InputMirrorSpec<?, ?> dispatchEnabledFacet : dispatchEnabledFacets) {
                Object inputValue = dispatchEnabledFacet.getFromRequest(originalRequest);
                if (!dispatchCase
                    .inputPredicates()
                    .getOrDefault(dispatchEnabledFacet, isAnyValue())
                    .matches(inputValue)) {
                  caseMatches = false;
                  break;
                }
              }
              if (caseMatches) {
                dispatchRequests
                    .computeIfAbsent(dispatchCase.dispatchTarget(), k -> new LinkedHashMap<>())
                    .put(requestId, originalRequest);
                dispatchTargetNotFound = false;
                break;
              }
            }
            if (dispatchTargetNotFound) {
              orphanedRequests.add(requestId);
            }
          }
          ImmutableList<VajramID> dispatchTargets = dynamicPolicy.dispatchTargets();
          for (VajramID dispatchTarget : dispatchTargets) {
            Map<RequestId, Request<?>> requestsForTarget =
                dispatchRequests.getOrDefault(dispatchTarget, Map.of());
            ClientSideCommand<BatchResponse> commandToDispatch;
            if (requestsForTarget.isEmpty()) {
              commandToDispatch =
                  new ForwardSend(
                      dispatchTarget,
                      ImmutableMap.of(),
                      forwardSend.dependantChain(),
                      executabledRequests.keySet().stream()
                          .collect(
                              toImmutableMap(
                                  identity(),
                                  _r ->
                                      "None of the requests to trait "
                                          + traitId
                                          + " matched "
                                          + dispatchTarget
                                          + " via dynamic predicate dispatch")));
            } else {
              commandToDispatch =
                  new ForwardSend(
                      dispatchTarget,
                      ImmutableMap.copyOf(requestsForTarget),
                      forwardSend.dependantChain(),
                      ImmutableMap.of());
            }
            dispatchResponses.put(
                dispatchTarget,
                (CompletableFuture<BatchResponse>)
                    invocationToDecorate.invokeDependency(
                        (ClientSideCommand<R>) commandToDispatch));
          }
          CompletableFuture<BatchResponse> mergedResponse = new CompletableFuture<>();
          allOf(dispatchResponses.values().stream().toArray(CompletableFuture[]::new))
              .whenComplete(
                  (unused, throwable) -> {
                    Map<RequestId, Errable<Object>> mergedResults = new LinkedHashMap<>();
                    for (Entry<VajramID, CompletableFuture<BatchResponse>> dispatchResponseEntry :
                        dispatchResponses.entrySet()) {
                      VajramID dispatchTarget = dispatchResponseEntry.getKey();
                      CompletableFuture<BatchResponse> dispatchResponse =
                          dispatchResponseEntry.getValue();
                      if (dispatchResponse.isCompletedExceptionally()) {
                        try {
                          dispatchResponse.join(); // Will throw exception
                        } catch (Throwable e) {
                          Set<RequestId> requestIds =
                              dispatchRequests.getOrDefault(dispatchTarget, Map.of()).keySet();
                          for (RequestId id : requestIds) {
                            mergedResults.put(id, Errable.withError(e));
                          }
                        }
                      } else {
                        BatchResponse resultValue = dispatchResponse.getNow(BatchResponse.empty());
                        mergedResults.putAll(resultValue.responses());
                      }
                    }
                    mergedResponse.complete(new BatchResponse(ImmutableMap.copyOf(mergedResults)));
                  });
          return (CompletableFuture<R>) mergedResponse;
        } else if (kryonCommand instanceof Flush flush) {
          List<CompletableFuture<FlushResponse>> flushResponses = new ArrayList<>();
          for (VajramID dispatchTarget : dynamicPolicy.dispatchTargets()) {
            CompletableFuture<FlushResponse> flushResponse =
                (CompletableFuture<FlushResponse>)
                    invocationToDecorate.invokeDependency(
                        (ClientSideCommand<R>) new Flush(dispatchTarget, flush.dependantChain()));
            flushResponses.add(flushResponse);
          }
          return (CompletableFuture<R>)
              allOf(flushResponses.toArray(CompletableFuture[]::new))
                  .handle((unused, throwable) -> FlushResponse.getInstance());
        } else {
          throw new IllegalStateException("Unknown command type: " + kryonCommand);
        }
      } else {
        throw new IllegalStateException("Unknown dispatch policy: " + traitDispatchPolicy);
      }
    };
  }

  private static <R extends KryonResponse> @Nullable
      ClientSideCommand<R> transformCommandForDispatch(
          ClientSideCommand<R> kryonCommand, VajramID boundVajram) {
    ClientSideCommand<R> commandToDispatch = null;
    if (kryonCommand instanceof ForwardSend forwardSend) {
      commandToDispatch =
          (ClientSideCommand<R>)
              new ForwardSend(
                  boundVajram,
                  forwardSend.executableRequests(),
                  forwardSend.dependantChain(),
                  forwardSend.skippedRequests());
    } else if (kryonCommand instanceof Flush) {
      commandToDispatch =
          (ClientSideCommand<R>) new Flush(boundVajram, kryonCommand.dependantChain());
    }
    return commandToDispatch;
  }
}
