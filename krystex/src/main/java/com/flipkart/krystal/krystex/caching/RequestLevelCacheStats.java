package com.flipkart.krystal.krystex.caching;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class RequestLevelCacheStats {
  private int cacheHits;
  private int cacheMisses;
  private int localCacheHits;
  private int localCacheMisses;
  private int globalCacheHits;
  private int globalCacheMissesNoFuture;
  private int globalCacheMissesIncompleteFuture;

  void localCacheHit() {
    localCacheHits++;
    cacheHits++;
  }

  void localCacheMiss() {
    localCacheMisses++;
    cacheMisses++;
  }

  void globalCacheHit() {
    globalCacheHits++;
    cacheHits++;
  }

  void globalCacheNoFuture() {
    globalCacheMissesNoFuture++;
    cacheMisses++;
  }

  void globalCacheNoFutureIncompleteFuture() {
    globalCacheMissesIncompleteFuture++;
    cacheMisses++;
  }
}
