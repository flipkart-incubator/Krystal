package com.flipkart.krystal.krystex.caching;

public record RequestLevelCacheStats(
    RequestLevelCacheKryonStats kryonStats, RequestLevelCacheOutputLogicStats outputLogicStats) {
  public RequestLevelCacheStats() {
    this(new RequestLevelCacheKryonStats(), new RequestLevelCacheOutputLogicStats());
  }
}
