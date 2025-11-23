package com.flipkart.krystal.vajramexecutor.krystex.traits;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.commands.ClientSideCommand;
import com.flipkart.krystal.krystex.commands.DirectForwardSend;
import com.flipkart.krystal.krystex.commands.ForwardSendBatch;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyInvocation;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.DirectResponse;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.flipkart.krystal.traits.DynamicDispatchPolicy;
import com.flipkart.krystal.traits.StaticDispatchPolicy;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;

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
  public <R extends KryonCommandResponse> DependencyInvocation<R> decorateDependency(
      DependencyInvocation<R> invocationToDecorate) {
    return kryonCommand -> {
      if (!vajramKryonGraph.getVajramDefinition(kryonCommand.vajramID()).isTrait()) {
        return invocationToDecorate.invokeDependency(kryonCommand);
      }
      VajramID traitId = kryonCommand.vajramID();
      Dependency dependency = kryonCommand.dependentChain().latestDependency();
      TraitDispatchPolicy traitDispatchPolicy = traitDispatchPolicies.get(traitId);
      if (traitDispatchPolicy instanceof StaticDispatchPolicy staticDispatchDefinition) {
        VajramID dispatchTarget;
        if (dependency != null) {
          dispatchTarget = staticDispatchDefinition.getDispatchTargetID(dependency);
        } else {
          throw new AssertionError(
              "This is not possible. A dependency decorator can only be invoked when there is a dependency present.");
        }
        ClientSideCommand<R> commandToDispatch = kryonCommand.rerouteTo(dispatchTarget);
        return invocationToDecorate.invokeDependency(commandToDispatch);
      } else if (traitDispatchPolicy instanceof DynamicDispatchPolicy dynamicPolicy) {
        if (kryonCommand instanceof ForwardSendBatch forwardSend) {
          var originalExecutableRequests = forwardSend.executableRequests();
          Map<InvocationId, String> originalSkippedInvocations = forwardSend.skippedInvocations();
          Map<VajramID, Map<InvocationId, Request<Object>>> dispatchRequests =
              new LinkedHashMap<>();
          Map<VajramID, CompletableFuture<BatchResponse>> dispatchResponses = new LinkedHashMap<>();
          Set<InvocationId> orphanedRequests = new LinkedHashSet<>();
          for (Entry<InvocationId, ? extends Request<Object>> requestEntry :
              originalExecutableRequests.entrySet()) {
            InvocationId invocationId = requestEntry.getKey();
            Request<Object> originalRequest = requestEntry.getValue();
            VajramID dispatchTarget =
                dynamicPolicy.getDispatchTargetID(dependency, originalRequest);
            if (dispatchTarget != null) {
              dispatchRequests
                  .computeIfAbsent(dispatchTarget, k -> new LinkedHashMap<>())
                  .put(invocationId, originalRequest);
            } else {
              orphanedRequests.add(invocationId);
            }
          }
          ImmutableSet<VajramID> dispatchTargets = dynamicPolicy.dispatchTargetIDs();
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
          for (VajramID dispatchTargetID : dispatchTargets) {
            Map<InvocationId, Request<Object>> requestsForTarget =
                dispatchRequests.getOrDefault(dispatchTargetID, Map.of());
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
                                      + dispatchTargetID
                                      + " via dynamic predicate dispatch")));
              skipRequests.putAll(requestsToSkip);

              commandToDispatch =
                  new ForwardSendBatch(
                      dispatchTargetID,
                      ImmutableMap.of(),
                      forwardSend.dependentChain(),
                      skipRequests);
            } else {
              commandToDispatch =
                  new ForwardSendBatch(
                      dispatchTargetID,
                      requestsForTarget,
                      forwardSend.dependentChain(),
                      requestsToSkip);
            }
            @SuppressWarnings("unchecked")
            CompletableFuture<BatchResponse> depResponse =
                (CompletableFuture<BatchResponse>)
                    invocationToDecorate.invokeDependency((ClientSideCommand<R>) commandToDispatch);
            dispatchResponses.put(dispatchTargetID, depResponse);
          }
          CompletableFuture<BatchResponse> mergedResponse = new CompletableFuture<>();
          allOf(dispatchResponses.values().toArray(CompletableFuture[]::new))
              .whenComplete(
                  (unused, throwable) -> {
                    Map<InvocationId, Errable<Object>> mergedResults = new LinkedHashMap<>();
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
        } else if (kryonCommand instanceof DirectForwardSend forwardSend) {
          List<? extends RequestResponseFuture<? extends Request<?>, ?>>
              originalExecutableRequests = forwardSend.executableRequests();
          Map<VajramID, List<RequestResponseFuture<? extends Request<?>, ?>>> dispatchRequests =
              new LinkedHashMap<>();
          for (RequestResponseFuture<? extends Request<?>, ?> requestEntry :
              originalExecutableRequests) {
            Request<?> originalRequest = requestEntry.request();
            VajramID dispatchTarget =
                dynamicPolicy.getDispatchTargetID(dependency, originalRequest);
            if (dispatchTarget != null) {
              dispatchRequests
                  .computeIfAbsent(
                      dispatchTarget, k -> new ArrayList<>(originalExecutableRequests.size()))
                  .add(requestEntry);
            } else {
              dispatchRequests
                  .computeIfAbsent(traitId, k -> new ArrayList<>(originalExecutableRequests.size()))
                  .add(requestEntry);
            }
          }
          ImmutableSet<Class<? extends Request<?>>> dispatchTargets =
              dynamicPolicy.dispatchTargetReqs();
          for (Class<? extends Request<?>> dispatchTarget : dispatchTargets) {
            VajramID dispatchTargetId = vajramKryonGraph.getVajramIdByVajramReqType(dispatchTarget);
            List<RequestResponseFuture<? extends Request<?>, ?>> requestsForTarget =
                dispatchRequests.getOrDefault(dispatchTargetId, List.of());
            ClientSideCommand<DirectResponse> commandToDispatch;
            commandToDispatch =
                new DirectForwardSend(
                    dispatchTargetId, requestsForTarget, forwardSend.dependentChain());

            @SuppressWarnings({"unchecked", "unused"})
            CompletableFuture<R> depResponse =
                invocationToDecorate.invokeDependency((ClientSideCommand<R>) commandToDispatch);
          }
          return completedFuture(DirectResponse.instance());
        } else {
          throw new IllegalStateException("Unknown command type: " + kryonCommand);
        }
      } else {
        throw new IllegalStateException("Unknown dispatch policy: " + traitDispatchPolicy);
      }
    };
  }
}
