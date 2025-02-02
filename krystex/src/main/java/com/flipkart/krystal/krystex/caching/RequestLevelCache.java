package com.flipkart.krystal.krystex.caching;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.allOf;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardReceive;
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
    cache.put(new CacheKey(new KryonId(kryonId), request._build()), data);
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
      if (kryonCommand instanceof ForwardReceive forwardBatch) {
        return readFromCache(kryon, forwardBatch);
      } else {
        // Let all other commands just pass through. Request level cache is supposed to intercept
        // ForwardBatch only.
        return kryon.executeCommand(kryonCommand);
      }
    }

    private CompletableFuture<KryonResponse> readFromCache(
        Kryon<KryonCommand, KryonResponse> kryon, ForwardReceive forwardBatch) {
      var executableRequests = forwardBatch.executableRequests();
      Map<RequestId, Facets> cacheMisses = new LinkedHashMap<>();
      Map<RequestId, CompletableFuture<@Nullable Object>> cacheHits = new LinkedHashMap<>();
      Map<RequestId, CompletableFuture<@Nullable Object>> newCacheEntries = new LinkedHashMap<>();
      executableRequests.forEach(
          (requestId, facets) -> {
            var cacheKey = new CacheKey(kryon.getKryonDefinition().kryonId(), facets._build());
            var cachedFuture = cache.get(cacheKey);
            if (cachedFuture == null) {
              var placeHolderFuture = new CompletableFuture<@Nullable Object>();
              newCacheEntries.put(requestId, placeHolderFuture);
              cache.put(cacheKey, placeHolderFuture);
              cacheMisses.put(requestId, facets._build());
            } else {
              cacheHits.put(requestId, cachedFuture);
            }
          });
      Map<RequestId, String> skippedRequests = new LinkedHashMap<>(forwardBatch.skippedRequests());
      cacheHits.forEach(
          (requestId, _f) -> skippedRequests.put(requestId, "Skipping due to cache hit!"));
      CompletableFuture<KryonResponse> cacheMissesResponse =
          kryon.executeCommand(
              new ForwardReceive(
                  forwardBatch.kryonId(),
                  ImmutableMap.copyOf(cacheMisses),
                  forwardBatch.dependantChain(),
                  ImmutableMap.copyOf(skippedRequests)));

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
      var allFuturesArray = new CompletableFuture[allFutures.size()];
      for (int i = 0; i < allFutures.size(); i++) {
        allFuturesArray[i] = allFutures.get(i).getValue();
      }
      allOf(allFuturesArray)
          .whenComplete(
              (unused, throwable) -> {
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
              });
      return finalResponse;
    }
  }
}
