package com.flipkart.krystal.krystex.caching;

import static com.flipkart.krystal.datatypes.Trilean.TRUE;

import com.flipkart.krystal.core.MutatesState;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableFacetValuesContainer;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.Trilean;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutorExecutionInfo;
import com.flipkart.krystal.krystex.resolution.FacetsFromRequest;
import com.flipkart.krystal.tags.ElementTags;
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
  private final Function<Class<? extends Request<?>>, VajramID> vajramIdMapper;
  private final Function<VajramID, ElementTags> vajramTagsProvider;
  private final KryonExecutorExecutionInfo executionInfo;

  public RequestLevelCacheInvalidator(
      RequestLevelCache requestLevelCache,
      Function<Class<? extends Request<?>>, VajramID> vajramIdMapper,
      Function<VajramID, ElementTags> vajramTagsProvider,
      KryonExecutorExecutionInfo executionInfo) {
    this.cacheContainer = requestLevelCache.cacheContainer();
    this.vajramIdMapper = vajramIdMapper;
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
      Class<R> reqClass, Predicate<FacetValues> invalidateIfTrue) {
    VajramID activeVajram = executionInfo.activeKryon();
    if (activeVajram == null) {
      log.error(
          "invalidateCacheKeys can only be called from an active vajram. Found no active vajram");
      return;
    }
    ElementTags vajramTags = vajramTagsProvider.apply(activeVajram);
    if (vajramTags == null) {
      log.error("Skipping cache invalidation: Found no vajramTags");
      return;
    }
    Optional<MutatesState> mutatesState = vajramTags.getAnnotationByType(MutatesState.class);
    if (mutatesState.isEmpty() || !mutatesState.get().value().equals(TRUE)) {
      log.error(
          "Request Level Cache invalidation can be done only from a @MutatesState(TRUE) vajram. For vajram {} found {}",
          activeVajram,
          mutatesState);
      return;
    }

    Optional<RequestLevelCacheConfig> requestLevelCacheConfig =
        vajramTags.getAnnotationByType(RequestLevelCacheConfig.class);
    if (requestLevelCacheConfig.isEmpty()) {
      log.error(
          "@RequestLevelCacheConfig missing on Vajram {} - this is mandatory for cache invalidation",
          activeVajram);
    }
    Class<? extends Request<?>>[] permittedTargetVajrams =
        requestLevelCacheConfig.get().canInvalidateCacheOf();

    boolean isPermitted = false;

    for (Class<? extends Request<?>> permittedTargetVajram : permittedTargetVajrams) {
      if (permittedTargetVajram.equals(reqClass)) {
        isPermitted = true;
        break;
      }
    }

    if (!isPermitted) {
      log.error(
          "Vajram {} has not declared intent to invalidate cache of vajram req {}. Please add it to @RequestLevelCacheConfig to allow this.",
          activeVajram,
          reqClass);
      return;
    }

    VajramID targetVajram;

    try {
      targetVajram = vajramIdMapper.apply(reqClass);
    } catch (Exception e) {
      log.warn(
          "Skipping cache invalidation as we could not retrieve vajramID for reqClass: {}",
          reqClass);
      return;
    }

    Iterator<ImmutableFacetValuesContainer> iterator =
        cacheContainer.getKeys(targetVajram).iterator();
    iterator.forEachRemaining(
        key -> {
          boolean shouldInvalidate;
          try {
            //noinspection unchecked
            shouldInvalidate = invalidateIfTrue.test((FacetValues) key);
          } catch (Exception e) {
            shouldInvalidate = false;
          }
          if (shouldInvalidate) {
            iterator.remove();
          }
        });
  }
}
