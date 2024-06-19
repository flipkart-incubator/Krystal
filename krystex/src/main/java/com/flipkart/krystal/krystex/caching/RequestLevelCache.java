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
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorationInput;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorator;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public class RequestLevelCache implements KryonDecorator {

  public static final String DECORATOR_TYPE = RequestLevelCache.class.getName();

  private static final Errable<Object> UNKNOWN_ERROR =
      Errable.withError(new StackTracelessException("Unknown error in request cache"));

  private final Map<CacheKey, CompletableFuture<@Nullable Object>> cache = new LinkedHashMap<>();

  @Override
  public Kryon<KryonCommand, KryonResponse> decorateKryon(KryonDecorationInput decorationInput) {
    return new CachingDecoratedKryon(decorationInput.kryon());
  }

  public void primeCache(String kryonId, Facets request, CompletableFuture<@Nullable Object> data) {
    cache.put(new CacheKey(new KryonId(kryonId), request), data);
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
      Map<RequestId, CompletableFuture<@Nullable Object>> cacheHits = new LinkedHashMap<>();
      executableRequests.forEach(
          (requestId, facets) -> {
            var cachedFuture =
                cache.get(new CacheKey(kryon.getKryonDefinition().kryonId(), facets));
            if (cachedFuture == null) {
              cacheMisses.put(requestId, facets);
            } else {
              cacheHits.put(requestId, cachedFuture);
            }
          });
      Map<RequestId, CompletableFuture<@Nullable Object>> newCacheEntries = new LinkedHashMap<>();
      Map<RequestId, String> skippedRequests = new LinkedHashMap<>(forwardBatch.skippedRequests());
      cacheHits.forEach(
          (requestId, _f) -> skippedRequests.put(requestId, "Skipping due to cache hit!"));
      CompletableFuture<KryonResponse> cacheMissesResponse =
          kryon.executeCommand(
              new ForwardBatch(
                  forwardBatch.kryonId(),
                  forwardBatch.inputNames(),
                  ImmutableMap.copyOf(cacheMisses),
                  forwardBatch.dependantChain(),
                  ImmutableMap.copyOf(skippedRequests)));
      cacheMisses.forEach(
          (requestId, facets) -> newCacheEntries.put(requestId, new CompletableFuture<>()));
      newCacheEntries.forEach(
          (requestId, cacheInsert) -> {
            Facets facets = cacheMisses.get(requestId);
            if (facets != null) {
              cache.put(new CacheKey(kryon.getKryonDefinition().kryonId(), facets), cacheInsert);
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
                    CompletableFuture<@Nullable Object> future = response.toFuture();
                    CompletableFuture<@Nullable Object> destinationFuture =
                        newCacheEntries.computeIfAbsent(
                            requestId, _r -> new CompletableFuture<@Nullable Object>());
                    linkFutures(future, destinationFuture);
                  });
            } else if (throwable != null) {
              cacheMisses.forEach(
                  (requestId, response) -> {
                    newCacheEntries
                        .computeIfAbsent(requestId, _r -> new CompletableFuture<@Nullable Object>())
                        .completeExceptionally(throwable);
                  });
            } else {
              RuntimeException e =
                  new RuntimeException("Exepecting BatchResponse. Found " + kryonResponse);
              log.error("", e);
              throw e;
            }
          });
      CompletableFuture<KryonResponse> finalResponse = new CompletableFuture<>();
      var allFutures =
          Stream.concat(cacheHits.entrySet().stream(), newCacheEntries.entrySet().stream())
              .toList();
      var array = new CompletableFuture[allFutures.size()];
      for (int i = 0; i < allFutures.size(); i++) {
        array[i] = allFutures.get(i).getValue();
      }
      CompletableFuture.allOf(array)
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
