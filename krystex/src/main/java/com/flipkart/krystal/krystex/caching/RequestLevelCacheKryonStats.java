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

  void localCacheHit() {
    localCacheHits++;
    cacheHits++;
  }

  void localCacheMiss() {
    localCacheMisses++;
  }

  void globalCacheCompletedFuture() {
    globalCacheHits++;
    cacheHits++;
  }

  void globalCacheNoFuture() {
    globalCacheMissesNoFuture++;
    cacheMisses++;
  }

  void globalCacheIncompleteFuture() {
    globalCacheMissesIncompleteFuture++;
    cacheMisses++;
  }

  void noCacheKey() {
    noCacheKey++;
    cacheMisses++;
  }
}
