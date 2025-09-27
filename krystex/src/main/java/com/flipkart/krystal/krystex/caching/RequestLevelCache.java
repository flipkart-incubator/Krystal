package com.flipkart.krystal.krystex.caching;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.flipkart.krystal.concurrent.Futures.propagateCompletion;
import static com.flipkart.krystal.except.StackTracelessException.stackTracelessWrap;
import static java.util.concurrent.CompletableFuture.allOf;

import com.flipkart.krystal.concurrent.Futures;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.krystex.commands.DirectForwardReceive;
import com.flipkart.krystal.krystex.commands.ForwardReceiveBatch;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.krystex.kryon.VajramKryonDefinition;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorationInput;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecorator;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public sealed class RequestLevelCache implements KryonDecorator, KryonExecutorConfigurator
    permits TestRequestLevelCache {

  public static final String DECORATOR_TYPE = RequestLevelCache.class.getName();

  private static final Errable<Object> UNKNOWN_ERROR =
      Errable.withError(new StackTracelessException("Unknown error in request cache"));

  private final Map<CacheKey, CompletableFuture<@Nullable Object>> cache = new LinkedHashMap<>();

  @Override
  public void addToConfig(KryonExecutorConfigBuilder configBuilder) {
    configBuilder.kryonDecoratorConfig(
        DECORATOR_TYPE,
        new KryonDecoratorConfig(
            DECORATOR_TYPE,
            _c -> true, // Apply cache to all vajrams
            _c -> DECORATOR_TYPE, // Only one RequestLevelCache across the vajram graph
            _c -> this // Reuse this instance across the graph
            ));
  }

  @Override
  public Kryon<KryonCommand, KryonCommandResponse> decorateKryon(
      KryonDecorationInput decorationInput) {
    return new CachingDecoratedKryon(decorationInput.kryon());
  }

  private class CachingDecoratedKryon implements Kryon<KryonCommand, KryonCommandResponse> {

    private final Kryon<KryonCommand, KryonCommandResponse> kryon;

    private CachingDecoratedKryon(Kryon<KryonCommand, KryonCommandResponse> kryon) {
      this.kryon = kryon;
    }

    @Override
    public VajramKryonDefinition getKryonDefinition() {
      return kryon.getKryonDefinition();
    }

    @Override
    public CompletableFuture<KryonCommandResponse> executeCommand(KryonCommand kryonCommand) {
      if (kryonCommand instanceof ForwardReceiveBatch forwardBatch) {
        return readFromCache(kryon, forwardBatch);
      } else if (kryonCommand instanceof DirectForwardReceive directForwardReceive) {
        return readFromCache(kryon, directForwardReceive);
      } else {
        // Let all other commands just pass through. Request level cache is supposed to intercept
        // ForwardBatch only.
        return kryon.executeCommand(kryonCommand);
      }
    }

    private CompletableFuture<KryonCommandResponse> readFromCache(
        Kryon<KryonCommand, KryonCommandResponse> kryon, DirectForwardReceive command) {
      List<ExecutionItem> cacheMisses = new ArrayList<>();
      for (ExecutionItem executionItem : command.executionItems()) {
        FacetValues facetValues = executionItem.facetValues();
        var cacheKey = new CacheKey(facetValues._build());
        var cachedFuture = getCachedValue(cacheKey);
        if (cachedFuture != null) {
          propagateCompletion(cachedFuture, executionItem.response());
        } else {
          cache.put(cacheKey, executionItem.response());
          cacheMisses.add(executionItem);
        }
      }
      return kryon.executeCommand(
          new DirectForwardReceive(command.vajramID(), cacheMisses, command.dependentChain()));
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private CompletableFuture<KryonCommandResponse> readFromCache(
        Kryon<KryonCommand, KryonCommandResponse> kryon, ForwardReceiveBatch forwardBatch) {
      var executableRequests = forwardBatch.executableInvocations();
      Map<InvocationId, FacetValues> cacheMisses = new LinkedHashMap<>();
      Map<InvocationId, CompletableFuture<@Nullable Object>> cacheHits = new LinkedHashMap<>();
      Map<InvocationId, CompletableFuture<@Nullable Object>> newCacheEntries =
          new LinkedHashMap<>();
      executableRequests.forEach(
          (requestId, facets) -> {
            var cacheKey = new CacheKey(facets._build());
            var cachedFuture = getCachedValue(cacheKey);
            if (cachedFuture == null) {
              var placeHolderFuture = new CompletableFuture<@Nullable Object>();
              newCacheEntries.put(requestId, placeHolderFuture);
              cache.put(cacheKey, placeHolderFuture);
              cacheMisses.put(requestId, facets._build());
            } else {
              cacheHits.put(requestId, cachedFuture);
            }
          });
      Map<InvocationId, String> skippedRequests =
          new LinkedHashMap<>(forwardBatch.invocationsToSkip());
      cacheHits.forEach(
          (requestId, _f) -> skippedRequests.put(requestId, "Skipping due to cache hit!"));
      CompletableFuture<KryonCommandResponse> cacheMissesResponse =
          kryon.executeCommand(
              new ForwardReceiveBatch(
                  forwardBatch.vajramID(),
                  cacheMisses,
                  forwardBatch.dependentChain(),
                  skippedRequests));

      cacheMissesResponse.whenComplete(
          (kryonResponse, throwable) -> {
            if (kryonResponse instanceof BatchResponse batchResponse) {
              Map<InvocationId, Errable<Object>> responses = batchResponse.responses();
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
                  (requestId, response) ->
                      newCacheEntries
                          .computeIfAbsent(
                              requestId, _r -> new CompletableFuture<@Nullable Object>())
                          .completeExceptionally(stackTracelessWrap(throwable)));
            } else {
              RuntimeException e =
                  new RuntimeException("Expecting BatchResponse. Found " + kryonResponse);
              log.error("", e);
              throw e;
            }
          });
      CompletableFuture<KryonCommandResponse> finalResponse = new CompletableFuture<>();
      Iterable<Entry<InvocationId, CompletableFuture<@Nullable Object>>> allFutures =
          Iterables.concat(cacheHits.entrySet(), newCacheEntries.entrySet());
      var allFuturesArray = new CompletableFuture[cacheHits.size() + newCacheEntries.size()];
      int i = 0;
      for (Entry<InvocationId, CompletableFuture<@Nullable Object>> e : allFutures) {
        allFuturesArray[i++] = e.getValue();
      }
      allOf(allFuturesArray)
          .whenComplete(
              (unused, throwable) -> {
                Map<InvocationId, Errable<Object>> responses = new LinkedHashMap<>();
                for (Entry<InvocationId, CompletableFuture<@Nullable Object>> e : allFutures) {
                  responses.put(
                      e.getKey(), e.getValue().handle(Errable::errableFrom).getNow(UNKNOWN_ERROR));
                }
                finalResponse.complete(new BatchResponse(responses));
              });
      return finalResponse;
    }
  }

  @Nullable CompletableFuture<@Nullable Object> getCachedValue(CacheKey cacheKey) {
    return cache.get(cacheKey);
  }

  void primeCache(FacetValues request, CompletableFuture<@Nullable Object> data) {
    cache.put(new CacheKey(request._build()), data);
  }
}
