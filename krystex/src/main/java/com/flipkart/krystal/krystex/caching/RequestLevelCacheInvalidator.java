package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.kryon.KryonExecutorExecutionInfo;
import com.flipkart.krystal.tags.ElementTags;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

/**
 * A facade for invalidating cache keys of a vajram. This is useful when a mutating vajram mutates
 * some state which is read by another vajram. The mutating vajram can invalidate
 */
@Slf4j
public class RequestLevelCacheInvalidator {

  private final CacheContainer cacheContainer;
  private final Function<VajramID, ElementTags> vajramTagsProvider;
  private final KryonExecutorExecutionInfo executionInfo;
  private final VajramGraph vajramGraph;

  public RequestLevelCacheInvalidator(
      RequestLevelCache requestLevelCache,
      Function<VajramID, ElementTags> vajramTagsProvider,
      KryonExecutorExecutionInfo executionInfo) {
    this.cacheContainer = requestLevelCache.cacheContainer();
    this.vajramGraph = requestLevelCache.vajramGraph();
    this.vajramTagsProvider = vajramTagsProvider;
    this.executionInfo = executionInfo;
  }

  /**
   * Invalidates cache keys for the vajram corresponding to the request class if the predicate
   * returns true
   *
   * @param reqClass the class representing the request of the vajram whose cache keys need to be
   *     invalidated
   * @param invalidateIfTrue a predicate that returns true if the cache key should be invalidated
   * @param <R> the type of the request
   */
  public <R extends Request<?>> void invalidateCacheKeys(
      Class<R> reqClass, Predicate<R> invalidateIfTrue) {
    VajramID activeVajram = executionInfo.activeKryon();
    if (activeVajram == null) {
      IllegalStateException e =
          new IllegalStateException(
              "InvalidateCacheKeys can only be called from an active vajram. Found no active vajram - possibly a framework bug?");
      log.error("", e);
      throw e;
    }
    ElementTags vajramTags = vajramTagsProvider.apply(activeVajram);
    Optional<RequestLevelCacheConfig> requestLevelCacheConfig = Optional.empty();
    if (vajramTags != null) {
      requestLevelCacheConfig = vajramTags.getAnnotationByType(RequestLevelCacheConfig.class);
    }
    if (vajramTags == null || requestLevelCacheConfig.isEmpty()) {
      IllegalStateException e =
          new IllegalStateException(
              "@RequestLevelCacheConfig missing on Vajram "
                  + activeVajram
                  + " - this is mandatory for cache invalidation");
      log.error("", e);
      throw e;
    }

    boolean isPermitted =
        Arrays.asList(requestLevelCacheConfig.get().canInvalidateCacheOf()).contains(reqClass);

    if (!isPermitted) {
      log.error(
          "Vajram {} has not declared intent to invalidate cache of vajram req {}. Please add it to @RequestLevelCacheConfig to allow this.",
          activeVajram,
          reqClass);
      return;
    }

    VajramID targetVajram;

    try {
      targetVajram = vajramGraph.getVajramIdByVajramReqType(reqClass);
    } catch (Exception e) {
      log.warn(
          "Skipping cache invalidation as we could not retrieve vajramID for reqClass: {}",
          reqClass);
      return;
    }

    Iterator<ImmutableFacetValues> iterator = cacheContainer.getKeys(targetVajram).iterator();
    iterator.forEachRemaining(
        facetValues -> {
          boolean shouldInvalidate;
          try {
            ImmutableRequest<?> immutableRequest = facetValues._request();
            if (reqClass.isInstance(immutableRequest)) {
              shouldInvalidate = invalidateIfTrue.test(reqClass.cast(immutableRequest));
            } else {
              shouldInvalidate = false;
            }
          } catch (Exception e) {
            shouldInvalidate = false;
          }
          if (shouldInvalidate) {
            iterator.remove();
          }
        });
  }
}
