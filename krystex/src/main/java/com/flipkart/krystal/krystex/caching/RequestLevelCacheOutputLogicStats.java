package com.flipkart.krystal.krystex.caching;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(doNotUseGetters = true)
public class RequestLevelCacheOutputLogicStats {
  private int cacheHits;
  private int cacheMisses;
  private int noCacheKey;

  void cacheHit() {
    cacheHits++;
  }

  void cacheMiss() {
    cacheMisses++;
  }

  public void noCacheKey() {
    noCacheKey++;
    cacheMisses++;
  }
}
