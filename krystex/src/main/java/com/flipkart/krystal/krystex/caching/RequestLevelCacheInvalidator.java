package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KrystalExecutorExecutionInfo;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.google.common.collect.Sets;
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
    VajramID sourceVajramId = executionInfo.activeVajram();
    if (sourceVajramId == null) {
      IllegalStateException e =
          new IllegalStateException(
              "InvalidateCacheKeys can only be called from an active vajram. Found no active vajram - possibly a framework bug?");
      log.error("", e);
      throw e;
    }
    VajramDefinition target;
    {
      Optional<VajramDefinition> vajramDefinitionOpt =
          vajramGraph.tryGetVajramDefinitionByReqType(targetReqType);
      if (vajramDefinitionOpt.isEmpty()) {
        log.warn(
            """
              Skipping cache invalidation as we could not retrieve vajramID for targetReqType: {}. \
              Maybe the vajram was not loaded into the graph?""",
            targetReqType);
        return;
      } else {
        target = vajramDefinitionOpt.get();
      }
    }
    boolean isPermitted = activeVajramMutatesTargetQueriedEntity(sourceVajramId, target);

    if (!isPermitted) {
      IllegalStateException e =
          new IllegalStateException(
              """
              Invalidation source vajram %s does not MUTATE a dataset which is \
              being queried by the invalidation target vajram %s. \
              Please declare appropriate @DataAccess annotations on both invalidating \
              and invalidated vajrams to allow this."""
                  .formatted(sourceVajramId, target.vajramId()));
      log.error("", e);
      throw e;
    }
    Iterator<ImmutableFacetValues> iterator = cacheContainer.getKeys(target.vajramId()).iterator();
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
            log.error(
                "{} could not invalidate cache keys of {} due to an exception.",
                sourceVajramId,
                target.vajramId(),
                e);
            throw e;
          }
          if (shouldInvalidate) {
            iterator.remove();
          }
        });
  }

  /** Returns true if the active vajram mutates an entity which is read by the target vajram. */
  private boolean activeVajramMutatesTargetQueriedEntity(
      VajramID activeVajramId, VajramDefinition target) {

    KryonDefinition targetKryonDef = vajramGraph.kryonDefinitionRegistry().get(target.vajramId());

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
}
