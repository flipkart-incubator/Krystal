package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ImmutableFacetValues;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

class CacheContainer {
  // To be used as OutputLogicDecorator
  private final Map<VajramID, Map<ImmutableFacetValues, CacheValue>> futureCache =
      new LinkedHashMap<>();

  void putFuture(ImmutableFacetValues key, CompletableFuture<@Nullable Object> value) {
    putFuture(
        key, value, 0 // If no epoch is given assign same value to all
        );
  }

  void putFuture(ImmutableFacetValues key, CompletableFuture<@Nullable Object> value, int epoch) {
    futureCache
        .computeIfAbsent(key._vajramID(), k -> new HashMap<>())
        .put(key, new CacheValue(value, epoch));
  }

  CacheValue getFuture(ImmutableFacetValues key) {
    return futureCache.getOrDefault(key._vajramID(), Map.of()).get(key);
  }

  @SuppressWarnings("return")
  Iterator<ImmutableFacetValues> getKeys(VajramID vajramID) {
    return futureCache.getOrDefault(vajramID, Map.of()).keySet().iterator();
  }

  record CacheValue(CompletableFuture<@Nullable Object> future, int epoch) {}
}
