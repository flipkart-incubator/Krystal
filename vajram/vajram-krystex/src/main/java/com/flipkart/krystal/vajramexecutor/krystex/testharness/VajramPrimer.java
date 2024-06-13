package com.flipkart.krystal.vajramexecutor.krystex.testharness;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardBatch;
import com.flipkart.krystal.krystex.commands.ForwardGranule;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorationInput;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorator;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.vajram.VajramRequest;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

/**
 * VajramPrimer is a custom Kryon Decorator which enables priming a response for a given request of
 * a vajram at runtime. When a kryon is decorated with vajram primer, it overrides the actual
 * execution logic of the vajram and replaces it with the provided execution stubs.
 */
@Slf4j
public class VajramPrimer implements KryonDecorator {

  private final Map<String, Map<Facets, Errable<Object>>> responsesByFacets;
  private final boolean failIfNotPrimed;

  public <T> VajramPrimer(
      Map<String, Map<VajramRequest<T>, Errable<T>>> primedResponses, boolean failIfNotPrimed) {

    this.failIfNotPrimed = failIfNotPrimed;
    this.responsesByFacets = new LinkedHashMap<>();
    primedResponses.forEach(
        (s, vajramRequestErrableMap) -> {
          vajramRequestErrableMap.forEach(
              (tVajramRequest, tErrable) -> {
                //noinspection unchecked
                responsesByFacets
                    .computeIfAbsent(s, _s -> new LinkedHashMap<>())
                    .computeIfAbsent(
                        tVajramRequest.toFacetValues(), _f -> (Errable<Object>) tErrable);
              });
        });
  }

  @Override
  public Kryon<KryonCommand, KryonResponse> decorateKryon(KryonDecorationInput decorationInput) {
    return new PrimingDecoratedKryon(decorationInput.kryon(), decorationInput.kryonExecutor());
  }

  private class PrimingDecoratedKryon implements Kryon<KryonCommand, KryonResponse> {

    private final Kryon<KryonCommand, KryonResponse> kryon;
    private final KryonExecutor kryonExecutor;

    private PrimingDecoratedKryon(
        Kryon<KryonCommand, KryonResponse> kryon, KryonExecutor kryonExecutor) {
      this.kryon = kryon;
      this.kryonExecutor = kryonExecutor;
    }

    @Override
    public void executeCommand(Flush flushCommand) {
      kryon.executeCommand(flushCommand);
    }

    @Override
    public CompletableFuture<KryonResponse> executeCommand(KryonCommand kryonCommand) {
      KryonId kryonId = kryonCommand.kryonId();
      if (kryonCommand instanceof ForwardBatch forwardBatch) {
        return getPrimedResponse(kryon, forwardBatch, kryonId, kryonExecutor);
      } else if (kryonCommand instanceof ForwardGranule) {
        throw new UnsupportedOperationException(
            "VajramPrimer does not support KryonExecStrategy GRANULAR. Please use BATCH instead");
      }
      return kryon.executeCommand(kryonCommand);
    }

    @Override
    public KryonDefinition getKryonDefinition() {
      return kryon.getKryonDefinition();
    }
  }

  private CompletableFuture<KryonResponse> getPrimedResponse(
      Kryon<KryonCommand, KryonResponse> kryon,
      ForwardBatch forwardBatch,
      KryonId kryonId,
      KryonExecutor kryonExecutor) {
    Map<RequestId, Errable<Object>> finalResponses = new LinkedHashMap<>();
    Set<RequestId> nonPrimedRequestIds = new LinkedHashSet<>();
    for (Entry<RequestId, Facets> entry : forwardBatch.executableRequests().entrySet()) {
      RequestId requestId = entry.getKey();
      Facets facets = entry.getValue();
      Errable<Object> primedResponse =
          responsesByFacets.getOrDefault(kryonId.value(), Map.of()).get(facets);
      if (primedResponse == null) {
        if (failIfNotPrimed) {
          throw new IllegalStateException(
              "Could not find primed response for inputs %s of kryon %s"
                  .formatted(facets, kryonId));
        } else {
          nonPrimedRequestIds.add(requestId);
        }
      } else {
        finalResponses.put(requestId, primedResponse);
      }
    }
    LinkedHashMap<RequestId, Facets> unprimedRequests =
        new LinkedHashMap<>(forwardBatch.executableRequests());
    unprimedRequests.keySet().retainAll(nonPrimedRequestIds);
    try {
      if (!unprimedRequests.isEmpty()) {
        CompletableFuture<KryonResponse> forwardedRequestsResult =
            kryon.executeCommand(
                new ForwardBatch(
                    forwardBatch.kryonId(),
                    forwardBatch.inputNames(),
                    ImmutableMap.copyOf(unprimedRequests),
                    forwardBatch.dependantChain(),
                    forwardBatch.skippedRequests()));
        return forwardedRequestsResult.handle(
            (kryonResponse, throwable) -> {
              if (kryonResponse instanceof BatchResponse batchResponse) {
                finalResponses.putAll(batchResponse.responses());
              } else {
                Errable<Object> error =
                    throwable != null
                        ? Errable.withError(throwable)
                        : Errable.withError(
                            new AssertionError(
                                "Unknown KryonResponse type of response %s from kryon %s"
                                    .formatted(kryonResponse, kryonId)));
                for (RequestId unprimedRequestId : nonPrimedRequestIds) {
                  finalResponses.put(unprimedRequestId, error);
                }
              }
              return new BatchResponse(ImmutableMap.copyOf(finalResponses));
            });
      } else {
        return completedFuture(new BatchResponse(ImmutableMap.copyOf(finalResponses)));
      }
    } finally {
      flushDependencies(kryon, forwardBatch, kryonExecutor);
    }
  }

  private void flushDependencies(
      Kryon<KryonCommand, KryonResponse> kryon,
      ForwardBatch forwardBatch,
      KryonExecutor kryonExecutor) {
    KryonDefinition kryonDefinition = kryon.getKryonDefinition();
    KryonId kryonId = kryonDefinition.kryonId();

    kryonDefinition
        .dependencyKryons()
        .forEach(
            (depName, depKryon) -> {
              KryonCommand kryonCommand =
                  new Flush(depKryon, forwardBatch.dependantChain().extend(kryonId, depName));
              kryonExecutor.executeCommand(kryonCommand);
            });
  }
}
