package com.flipkart.krystal.krystex.caching;

import static com.flipkart.krystal.utils.Futures.linkFutures;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardBatch;
import com.flipkart.krystal.krystex.commands.ForwardGranule;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorationContext;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorator;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestLevelCache implements KryonDecorator {

  private static final Errable<Object> UNKNOWN_ERROR =
      Errable.withError(new StackTracelessException("Unknown error in request cache"));

  private CachingDecoratedKryon kryon;
  private Map<Facets, CompletableFuture<Object>> cache = new LinkedHashMap<>();

  @Override
  public Kryon<KryonCommand, KryonResponse> decorateKryon(KryonDecorationContext context) {
    if (kryon == null) {
      kryon = new CachingDecoratedKryon(kryon);
    }
    return kryon;
  }

  private class CachingDecoratedKryon implements Kryon<KryonCommand, KryonResponse> {

    private final Kryon<KryonCommand, KryonResponse> kryon;

    private CachingDecoratedKryon(Kryon<KryonCommand, KryonResponse> kryon) {
      this.kryon = kryon;
    }

    @Override
    public void executeCommand(Flush flushCommand) {
      kryon.executeCommand(flushCommand);
    }

    @Override
    public KryonDefinition getKryonDefinition() {
      return kryon.getKryonDefinition();
    }

    @Override
    public CompletableFuture<KryonResponse> executeCommand(KryonCommand kryonCommand) {
      if (kryonCommand instanceof ForwardBatch forwardBatch) {
        return readFromCache(kryon, forwardBatch);
      } else if (kryonCommand instanceof ForwardGranule) {
        var e =
            new UnsupportedOperationException(
                "KryonInputInjector does not support KryonExecStrategy GRANULAR. Please use BATCH instead");
        log.error("", e);
        throw e;
      }
      return kryon.executeCommand(kryonCommand);
    }

    private CompletableFuture<KryonResponse> readFromCache(
        Kryon<KryonCommand, KryonResponse> kryon, ForwardBatch forwardBatch) {
      ImmutableMap<RequestId, Facets> executableRequests = forwardBatch.executableRequests();
      Map<RequestId, Facets> cacheMisses = new LinkedHashMap<>();
      Map<RequestId, CompletableFuture<Object>> cacheHits = new LinkedHashMap<>();
      executableRequests.forEach(
          (requestId, facets) -> {
            CompletableFuture<Object> cachedFuture = cache.get(facets);
            if (cachedFuture == null) {
              cacheMisses.put(requestId, facets);
            } else {
              cacheHits.put(requestId, cachedFuture);
            }
          });
      Map<RequestId, CompletableFuture<Object>> newCacheEntries = new LinkedHashMap<>();
      if (!cacheMisses.isEmpty()) {
        CompletableFuture<KryonResponse> cacheMissesResponse =
            kryon.executeCommand(
                new ForwardBatch(
                    forwardBatch.kryonId(),
                    forwardBatch.inputNames(),
                    ImmutableMap.copyOf(cacheMisses),
                    forwardBatch.dependantChain(),
                    forwardBatch.skippedRequests()));
        cacheMisses.forEach(
            (requestId, facets) -> newCacheEntries.put(requestId, new CompletableFuture<>()));
        newCacheEntries.forEach(
            (requestId, cacheInsert) -> {
              Facets facets = cacheMisses.get(requestId);
              if (facets != null) {
                cache.put(facets, cacheInsert);
              } else {
                var e =
                    new AssertionError(
                        "This should not happen since requestId will definitely be there in the cacheMisses map");
                log.error("", e);
                throw e;
              }
            });

        cacheMissesResponse.whenComplete(
            (kryonResponse, throwable) -> {
              if (kryonResponse instanceof BatchResponse batchResponse) {
                ImmutableMap<RequestId, Errable<Object>> responses = batchResponse.responses();
                responses.forEach(
                    (requestId, response) -> {
                      linkFutures(
                          response.toFuture(),
                          newCacheEntries.computeIfAbsent(
                              requestId, _r -> new CompletableFuture<>()));
                    });
              } else if (throwable != null) {
                cacheMisses.forEach(
                    (requestId, response) -> {
                      newCacheEntries
                          .computeIfAbsent(requestId, _r -> new CompletableFuture<>())
                          .completeExceptionally(throwable);
                    });
              } else {
                RuntimeException e =
                    new RuntimeException("Exepecting BatchResponse. Found " + kryonResponse);
                log.error("", e);
                throw e;
              }
            });
      }
      CompletableFuture<KryonResponse> finalResponse = new CompletableFuture<>();
      List<Entry<RequestId, CompletableFuture<Object>>> allFutures =
          Stream.concat(cacheHits.entrySet().stream(), newCacheEntries.entrySet().stream())
              .toList();
      CompletableFuture.allOf(
              allFutures.stream().map(Entry::getValue).toArray(CompletableFuture[]::new))
          .whenComplete(
              (unused, throwable) -> {
                if (throwable != null) {
                  finalResponse.completeExceptionally(throwable);
                } else {
                  finalResponse.complete(
                      new BatchResponse(
                          allFutures.stream()
                              .collect(
                                  toImmutableMap(
                                      Entry::getKey,
                                      entry ->
                                          entry
                                              .getValue()
                                              .handle(Errable::errableFrom)
                                              .getNow(UNKNOWN_ERROR)))));
                }
              });
      return finalResponse;
    }
  }
}
