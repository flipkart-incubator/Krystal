package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KrystalExecutorExecutionInfo;
import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

/**
 * A facade for invalidating cache keys of a vajram. This is useful when a mutating vajram mutates
 * some state which is read by another vajram. The mutating vajram can invalidate
 */
@Slf4j
public class RequestLevelCacheInvalidator {

  private final CacheContainer cacheContainer;
  private final KrystalExecutorExecutionInfo executionInfo;
  private final VajramGraph vajramGraph;

  public RequestLevelCacheInvalidator(
      RequestLevelCache requestLevelCache, KrystalExecutorExecutionInfo executionInfo) {
    this.cacheContainer = requestLevelCache.cacheContainer();
    this.vajramGraph = requestLevelCache.vajramGraph();
    this.executionInfo = executionInfo;
  }

  /**
   * Invalidates cache keys for the vajram corresponding to the request class if the predicate
   * returns true
   *
   * @param targetReqType the class representing the request of the vajram whose cache keys need to
   *     be invalidated
   * @param invalidateIfTrue a predicate that returns true if the cache key should be invalidated
   * @param <R> the type of the request
   */
  public <R extends Request<?>> void invalidateCacheKeys(
      Class<R> targetReqType, Predicate<R> invalidateIfTrue) {
    VajramID activeVajramId = executionInfo.activeVajram();
    if (activeVajramId == null) {
      IllegalStateException e =
          new IllegalStateException(
              "InvalidateCacheKeys can only be called from an active vajram. Found no active vajram - possibly a framework bug?");
      log.error("", e);
      throw e;
    }
    VajramDefinition activeVajram = vajramGraph.getVajramDefinition(activeVajramId);
    ElementTags directTags = activeVajram.vajramTags();
    Optional<RequestLevelCacheConfig> requestLevelCacheConfig =
        directTags.getAnnotationByType(RequestLevelCacheConfig.class);

    boolean isPermitted =
        (requestLevelCacheConfig.isPresent()
                && canInvalidateCacheOf(targetReqType, requestLevelCacheConfig.get()))
            || activeVajramMutatesTargetQueriedEntity(activeVajramId, targetReqType);

    if (!isPermitted) {
      IllegalStateException e =
          new IllegalStateException(
              """
              Vajram has not declared intent to invalidate cache of the provided vajram req. \
              Either declare @EntityAccess on invalidator and invalidated vajrams or declare the \
              request type in @RequestLevelCacheConfig to allow this.""");
      log.error("", e);
      throw e;
    }

    VajramID targetVajram;

    try {
      targetVajram = vajramGraph.getVajramIdByVajramReqType(targetReqType);
    } catch (Exception e) {
      log.warn(
          "Skipping cache invalidation as we could not retrieve vajramID for targetReqType: {}",
          targetReqType);
      return;
    }

    Iterator<ImmutableFacetValues> iterator = cacheContainer.getKeys(targetVajram).iterator();
    iterator.forEachRemaining(
        facetValues -> {
          boolean shouldInvalidate;
          try {
            ImmutableRequest<?> immutableRequest = facetValues._request();
            if (targetReqType.isInstance(immutableRequest)) {
              shouldInvalidate = invalidateIfTrue.test(targetReqType.cast(immutableRequest));
            } else {
              shouldInvalidate = false;
            }
          } catch (Exception e) {
            log.error("Could not invalidate cache keys due to an exception.", e);
            shouldInvalidate = false;
          }
          if (shouldInvalidate) {
            iterator.remove();
          }
        });
  }

  /** Returns true of the active vajram mutates an entity which is read by the target vajram. */
  private <R extends Request<?>> boolean activeVajramMutatesTargetQueriedEntity(
      VajramID activeVajramId, Class<R> targetReqType) {
    Optional<VajramDefinition> targetVajramOpt =
        vajramGraph.tryGetVajramDefinitionByReqType(targetReqType);
    if (targetVajramOpt.isEmpty()) {
      // This happens if the vajram was not loaded into the graph
      // In this case we assume that an entity is shared as it's anyway a no-op
      return true;
    }

    KryonDefinition targetKryonDef =
        vajramGraph.kryonDefinitionRegistry().get(targetVajramOpt.get().vajramId());

    if (targetKryonDef == null) {
      // This can happen if the kryon definition was not loaded into the graph
      // In this case we assume that an entity is shared as it's anyway a no-op
      return true;
    }

    KryonDefinition currentKryonDef =
        vajramGraph.kryonDefinitionRegistry().getOrThrow(activeVajramId);

    KryonCachingMetadata currentKryonMetadata =
        currentKryonDef.getCustomMetadata(
            KryonCachingMetadata.class,
            kryonDefinition ->
                RequestLevelCache.computeCachingMetadata(kryonDefinition, vajramGraph));

    KryonCachingMetadata targetKryonMetadata =
        targetKryonDef.getCustomMetadata(
            KryonCachingMetadata.class,
            kryonDefinition ->
                RequestLevelCache.computeCachingMetadata(kryonDefinition, vajramGraph));

    return !Sets.intersection(
            currentKryonMetadata.entitiesMutated(), targetKryonMetadata.entitiesQueried())
        .isEmpty();
  }

  private static <R extends Request<?>> boolean canInvalidateCacheOf(
      Class<R> reqClass, RequestLevelCacheConfig requestLevelCacheConfig) {
    return Arrays.asList(requestLevelCacheConfig.canInvalidateCacheOf()).contains(reqClass);
  }
}
