package com.flipkart.krystal.krystex.caching;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(doNotUseGetters = true)
public class RequestLevelCacheKryonStats {
  private int cacheHits;
  private int cacheMisses;
  private int localCacheHits;
  private int localCacheMisses;
  private int globalCacheHits;
  private int globalCacheMissesNoFuture;
  private int globalCacheMissesIncompleteFuture;
  private int noCacheKey;
  private int globalCacheHitsCompletedFuture;
  private int globalCacheHitsIncompleteFuture;

  void localCacheHit() {
    localCacheHits++;
    cacheHits++;
  }

  void localCacheMiss() {
    localCacheMisses++;
  }

  void globalCacheHitsCompletedFuture() {
    globalCacheHits++;
    globalCacheHitsCompletedFuture++;
    cacheHits++;
  }

  void globalCacheHitsIncompleteFuture() {
    globalCacheHitsIncompleteFuture++;
    globalCacheHits++;
    cacheHits++;
  }

  void globalCacheNoFuture() {
    globalCacheMissesNoFuture++;
    cacheMisses++;
  }

  void globalCacheMissIncompleteFuture() {
    globalCacheMissesIncompleteFuture++;
    cacheMisses++;
  }

  void noCacheKey() {
    noCacheKey++;
    cacheMisses++;
  }
}
