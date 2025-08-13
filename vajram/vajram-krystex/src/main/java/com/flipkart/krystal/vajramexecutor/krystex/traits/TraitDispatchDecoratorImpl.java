package com.flipkart.krystal.vajramexecutor.krystex.traits;

import static com.flipkart.krystal.traits.matchers.InputValueMatcher.isAnyValue;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.commands.ClientSideCommand;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardSend;
import com.flipkart.krystal.krystex.commands.VoidResponse;
import com.flipkart.krystal.krystex.dependencydecoration.VajramInvocation;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.request.InvocationId;
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
  @SuppressWarnings("FutureReturnValueIgnored")
  public <R extends KryonCommandResponse> VajramInvocation<R> decorateDependency(
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
        Dependency dependency = kryonCommand.dependentChain().latestDependency();
        if (dependency != null) {
          boundVajram = staticDispatchDefinition.get(dependency);
        } else {
          throw new AssertionError(
              "This is not possible. A dependency decorator can only be invoked when there is a dependency present.");
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
          var originalExecutableRequests = forwardSend.executableRequests();
          Map<InvocationId, String> originalSkippedInvocations = forwardSend.skippedInvocations();
          Map<VajramID, Map<InvocationId, Request<@Nullable Object>>> dispatchRequests =
              new LinkedHashMap<>();
          Map<VajramID, CompletableFuture<BatchResponse>> dispatchResponses = new LinkedHashMap<>();
          Set<InvocationId> orphanedRequests = new LinkedHashSet<>();
          for (Entry<InvocationId, ? extends Request<@Nullable Object>> requestEntry :
              originalExecutableRequests.entrySet()) {
            InvocationId invocationId = requestEntry.getKey();
            Request<@Nullable Object> originalRequest = requestEntry.getValue();
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
                    .put(invocationId, originalRequest);
                dispatchTargetNotFound = false;
                break;
              }
            }
            if (dispatchTargetNotFound) {
              orphanedRequests.add(invocationId);
            }
          }
          ImmutableList<VajramID> dispatchTargets = dynamicPolicy.dispatchTargets();
          ImmutableMap<InvocationId, String> requestsToSkip =
              ImmutableMap.<InvocationId, String>builder()
                  .putAll(originalSkippedInvocations)
                  .putAll(
                      orphanedRequests.stream()
                          .collect(
                              toMap(
                                  identity(),
                                  _r ->
                                      "The request did not match any of the configured dynamic dispatch targets of trait: "
                                          + traitId)))
                  .build();
          for (VajramID dispatchTarget : dispatchTargets) {
            Map<InvocationId, Request<@Nullable Object>> requestsForTarget =
                dispatchRequests.getOrDefault(dispatchTarget, Map.of());
            ClientSideCommand<BatchResponse> commandToDispatch;
            if (requestsForTarget.isEmpty()) {
              Map<InvocationId, String> skipRequests = new LinkedHashMap<>();
              skipRequests.putAll(
                  originalExecutableRequests.keySet().stream()
                      .collect(
                          toMap(
                              identity(),
                              _r ->
                                  "None of the requests to trait "
                                      + traitId
                                      + " matched "
                                      + dispatchTarget
                                      + " via dynamic predicate dispatch")));
              skipRequests.putAll(requestsToSkip);

              commandToDispatch =
                  new ForwardSend(
                      dispatchTarget,
                      ImmutableMap.of(),
                      forwardSend.dependentChain(),
                      skipRequests);
            } else {
              commandToDispatch =
                  new ForwardSend(
                      dispatchTarget,
                      requestsForTarget,
                      forwardSend.dependentChain(),
                      requestsToSkip);
            }
            @SuppressWarnings("unchecked")
            CompletableFuture<BatchResponse> depResponse =
                (CompletableFuture<BatchResponse>)
                    invocationToDecorate.invokeDependency((ClientSideCommand<R>) commandToDispatch);
            dispatchResponses.put(dispatchTarget, depResponse);
          }
          CompletableFuture<BatchResponse> mergedResponse = new CompletableFuture<>();
          allOf(dispatchResponses.values().toArray(CompletableFuture[]::new))
              .whenComplete(
                  (unused, throwable) -> {
                    Map<InvocationId, Errable<@Nullable Object>> mergedResults =
                        new LinkedHashMap<>();
                    for (Entry<VajramID, CompletableFuture<BatchResponse>> dispatchResponseEntry :
                        dispatchResponses.entrySet()) {
                      VajramID dispatchTarget = dispatchResponseEntry.getKey();
                      CompletableFuture<BatchResponse> dispatchResponse =
                          dispatchResponseEntry.getValue();
                      if (dispatchResponse.isCompletedExceptionally()) {
                        try {
                          dispatchResponse.join(); // Will throw exception
                        } catch (Throwable e) {
                          Set<InvocationId> invocationIds =
                              dispatchRequests.getOrDefault(dispatchTarget, Map.of()).keySet();
                          for (InvocationId id : invocationIds) {
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
          @SuppressWarnings("unchecked")
          CompletableFuture<R> castMergedResponse = (CompletableFuture<R>) mergedResponse;
          return castMergedResponse;
        } else if (kryonCommand instanceof Flush flush) {
          List<CompletableFuture<VoidResponse>> flushResponses = new ArrayList<>();
          for (VajramID dispatchTarget : dynamicPolicy.dispatchTargets()) {
            @SuppressWarnings("unchecked")
            CompletableFuture<VoidResponse> flushResponse =
                (CompletableFuture<VoidResponse>)
                    invocationToDecorate.invokeDependency(
                        (ClientSideCommand<R>) new Flush(dispatchTarget, flush.dependentChain()));
            flushResponses.add(flushResponse);
          }
          @SuppressWarnings("unchecked")
          CompletableFuture<R> flushResponse =
              (CompletableFuture<R>)
                  allOf(flushResponses.toArray(CompletableFuture[]::new))
                      .handle((unused, throwable) -> VoidResponse.getInstance());
          return flushResponse;
        } else {
          throw new IllegalStateException("Unknown command type: " + kryonCommand);
        }
      } else {
        throw new IllegalStateException("Unknown dispatch policy: " + traitDispatchPolicy);
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static <R extends KryonCommandResponse> @Nullable
      ClientSideCommand<R> transformCommandForDispatch(
          ClientSideCommand<R> kryonCommand, VajramID boundVajram) {
    ClientSideCommand<R> commandToDispatch = null;
    if (kryonCommand instanceof ForwardSend forwardSend) {
      commandToDispatch =
          (ClientSideCommand<R>)
              new ForwardSend(
                  boundVajram,
                  forwardSend.executableRequests(),
                  forwardSend.dependentChain(),
                  forwardSend.skippedInvocations());
    } else if (kryonCommand instanceof Flush) {
      commandToDispatch =
          (ClientSideCommand<R>) new Flush(boundVajram, kryonCommand.dependentChain());
    }
    return commandToDispatch;
  }
}
