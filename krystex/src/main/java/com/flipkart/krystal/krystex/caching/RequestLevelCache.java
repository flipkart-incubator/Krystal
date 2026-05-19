package com.flipkart.krystal.krystex.caching;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.flipkart.krystal.concurrent.Futures.propagateCompletion;
import static com.flipkart.krystal.datatypes.Trilean.UNKNOWN;
import static com.flipkart.krystal.except.KrystalCompletionException.wrapAsCompletionException;
import static java.util.concurrent.CompletableFuture.allOf;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.data.MutatesState;
import com.flipkart.krystal.except.KrystalCompletionException;
import com.flipkart.krystal.krystex.commands.DirectForwardReceive;
import com.flipkart.krystal.krystex.commands.ForwardReceiveBatch;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator.KryonExecutorConfiguratorProvider;
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
public sealed class RequestLevelCache implements KryonDecorator, KryonExecutorConfiguratorProvider
    permits TestRequestLevelCache {

  public static final String DECORATOR_TYPE = RequestLevelCache.class.getName();

  private static final Errable<Object> UNKNOWN_ERROR =
      Errable.withError(new KrystalCompletionException("Unknown error in request cache"));

  private final CacheContainer cache = new CacheContainer();
  private final KryonDefinitionRegistry kryonDefinitionRegistry;
  private final boolean defaultMutatesStateVal;

  /**
   * If a vajram doesn't have a @MutatesState annotation, then it is assumed to mutate state, and
   * caching is skipped.
   *
   * @param kryonDefinitionRegistry the Kryon Definition registry corresponding to the Krystal
   *     executor for which this is a request level cache
   */
  public RequestLevelCache(KryonDefinitionRegistry kryonDefinitionRegistry) {
    this(kryonDefinitionRegistry, true);
  }

  /**
   * @param kryonDefinitionRegistry the Kryon Definition registry corresponding to the Krystal
   *     executor for which this is a request level cache
   * @param defaultMutatesStateVal If a vajram doesn't have a @MutatesState annotation, then this
   *     value is used as the default. NOTE: Passing "false" here is not recommended as it can lead
   *     to unexpected behavior. This has been provided to support legacy code, and would be removed
   *     in a future release. Prefer using the other constructor which defaults to true.
   */
  public RequestLevelCache(
      KryonDefinitionRegistry kryonDefinitionRegistry, boolean defaultMutatesStateVal) {
    this.kryonDefinitionRegistry = kryonDefinitionRegistry;
    this.defaultMutatesStateVal = defaultMutatesStateVal;
  }

  @Override
  public KryonExecutorConfigurator asKryonExecutorConfigurator() {
    return configBuilder ->
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
  public Kryon<KryonCommand<?>, KryonCommandResponse> decorateKryon(
      KryonDecorationInput decorationInput) {
    return new CachingDecoratedKryon(decorationInput.kryon());
  }

  CacheContainer cacheContainer() {
    return cache;
  }

  private class CachingDecoratedKryon implements Kryon<KryonCommand<?>, KryonCommandResponse> {

    private final Kryon<KryonCommand<?>, KryonCommandResponse> kryon;

    private CachingDecoratedKryon(Kryon<KryonCommand<?>, KryonCommandResponse> kryon) {
      this.kryon = kryon;
    }

    @Override
    public VajramKryonDefinition getKryonDefinition() {
      return kryon.getKryonDefinition();
    }

    @Override
    public CompletableFuture<KryonCommandResponse> executeCommand(KryonCommand<?> kryonCommand) {
      if (kryonCommand instanceof ForwardReceiveBatch forwardBatch
          && isEligibleForCaching(forwardBatch)) {
        return readFromCache(kryon, forwardBatch);
      } else if (kryonCommand instanceof DirectForwardReceive directForwardReceive
          && isEligibleForCaching(directForwardReceive)) {
        return readFromCache(kryon, directForwardReceive);
      } else {
        // Let all other commands just pass through. Request level cache is supposed to intercept
        // only
        // Forward Commands, and only for eligible vajrams.
        return kryon.executeCommand(kryonCommand);
      }
    }

    private boolean isEligibleForCaching(KryonCommand<?> kryonCommand) {
      VajramID vajramID = kryonCommand.vajramID();
      KryonDefinition kryonDefinition = kryonDefinitionRegistry.getOrThrow(vajramID);
      boolean mutatesStateTransitive =
          kryonDefinition
              .tags()
              .getAnnotationByType(MutatesState.class)
              .map(MutatesState::value)
              .orElse(UNKNOWN)
              .asBoolean(defaultMutatesStateVal);
      return !mutatesStateTransitive;
    }

    private CompletableFuture<KryonCommandResponse> readFromCache(
        Kryon<KryonCommand<?>, KryonCommandResponse> kryon, DirectForwardReceive command) {
      List<ExecutionItem> cacheMisses = new ArrayList<>();
      for (ExecutionItem executionItem : command.executionItems()) {
        FacetValues facetValues = executionItem.facetValues();
        var cacheKey = newCacheKey(facetValues);
        if (cacheKey == null) {
          // Since the cache key could not be generated, we skip caching for this request
          log.error(
              "Skipping DirectForwardReceive caching for request {} since cache key is null",
              facetValues);
          cacheMisses.add(executionItem);
          continue;
        }
        var cachedFuture = getCachedValue(cacheKey);
        if (cachedFuture == null) {
          cache.put(cacheKey, executionItem.response());
          cacheMisses.add(executionItem);
          continue;
        }
        propagateCompletion(cachedFuture, executionItem.response());
      }
      return kryon.executeCommand(
          new DirectForwardReceive(command.vajramID(), cacheMisses, command.dependentChain()));
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private CompletableFuture<KryonCommandResponse> readFromCache(
        Kryon<KryonCommand<?>, KryonCommandResponse> kryon, ForwardReceiveBatch forwardBatch) {
      var executableRequests = forwardBatch.executableInvocations();
      Map<InvocationId, FacetValues> cacheMisses = new LinkedHashMap<>();
      Map<InvocationId, CompletableFuture<@Nullable Object>> cacheHits = new LinkedHashMap<>();
      Map<InvocationId, CompletableFuture<@Nullable Object>> newCacheEntries =
          new LinkedHashMap<>();
      executableRequests.forEach(
          (requestId, facets) -> {
            ImmutableFacetValues cacheKey = newCacheKey(facets);
            if (cacheKey == null) {
              // Since the cache key could not be generated, we skip caching for this request
              log.error(
                  "Skipping forwardBatch caching for request {} since cache key is null", facets);
              cacheMisses.put(requestId, facets);
              return;
            }
            var cachedFuture = getCachedValue(cacheKey);
            if (cachedFuture == null) {
              var placeHolderFuture = new CompletableFuture<@Nullable Object>();
              newCacheEntries.put(requestId, placeHolderFuture);
              cache.put(cacheKey, placeHolderFuture);
              cacheMisses.put(requestId, cacheKey);
            } else {
              cacheHits.put(requestId, cachedFuture);
            }
          });
      CompletableFuture<KryonCommandResponse> cacheMissesResponse =
          kryon.executeCommand(
              new ForwardReceiveBatch(
                  forwardBatch.vajramID(), cacheMisses, forwardBatch.dependentChain()));

      cacheMissesResponse.whenComplete(
          (kryonResponse, throwable) -> {
            if (kryonResponse instanceof BatchResponse batchResponse) {
              Map<InvocationId, Errable<Object>> responses = batchResponse.responses();
              responses.forEach(
                  (requestId, response) -> {
                    CompletableFuture<? extends @Nullable Object> future = response.toFuture();
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
                          .completeExceptionally(wrapAsCompletionException(throwable)));
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

  /**
   * We use facets instead of request as a cache key so that some non-input facets which have been
   * specifically added (for example, injections) to act as cache keys are also taken into account.
   * This, however, is a rare use case and in most cases, all non-input facets are null at the time
   * of cache key computation and using the facets object is equivalent to using the inner request
   * object.
   *
   * @param facetValues
   */
  private static @Nullable ImmutableFacetValues newCacheKey(FacetValues facetValues) {
    ImmutableFacetValues immut;
    try {
      immut = facetValues._build();
    } catch (Exception e) {
      log.error(
          "Unable to generate cache key by 'building' facet values to create an Immutable instance as an exception was encountered while building.",
          e);
      return null;
    }
    return immut;
  }

  @Nullable CompletableFuture<@Nullable Object> getCachedValue(ImmutableFacetValues cacheKey) {
    return cache.get(cacheKey);
  }

  void primeCache(FacetValues facetValues, CompletableFuture<@Nullable Object> data) {
    cache.put(facetValues._build(), data);
  }
}
