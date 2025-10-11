package com.flipkart.krystal.vajramexecutor.krystex.traits;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

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
      if (!vajramKryonGraph.getVajramDefinition(kryonCommand.vajramID()).isTrait()) {
        return invocationToDecorate.invokeDependency(kryonCommand);
      }
      VajramID traitId = kryonCommand.vajramID();
      Dependency dependency = kryonCommand.dependentChain().latestDependency();
      TraitDispatchPolicy traitDispatchPolicy = traitDispatchPolicies.get(traitId);
      if (traitDispatchPolicy instanceof StaticDispatchPolicy staticDispatchDefinition) {
        VajramID dispatchTarget;
        ClientSideCommand<R> commandToDispatch;
        if (dependency != null) {
          dispatchTarget = staticDispatchDefinition.getDispatchTargetID(dependency);
        } else {
          throw new AssertionError(
              "This is not possible. A dependency decorator can only be invoked when there is a dependency present.");
        }
        commandToDispatch = transformCommandForDispatch(kryonCommand, dispatchTarget);
        if (commandToDispatch == null) {
          commandToDispatch = kryonCommand;
        }
        return invocationToDecorate.invokeDependency(commandToDispatch);
      } else if (traitDispatchPolicy instanceof DynamicDispatchPolicy dynamicPolicy) {
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
            @Nullable VajramID dispatchTarget =
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
            Map<InvocationId, Request<@Nullable Object>> requestsForTarget =
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
                  new ForwardSend(
                      dispatchTargetID,
                      ImmutableMap.of(),
                      forwardSend.dependentChain(),
                      skipRequests);
            } else {
              commandToDispatch =
                  new ForwardSend(
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
          for (VajramID dispatchTarget : dynamicPolicy.dispatchTargetIDs()) {
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
