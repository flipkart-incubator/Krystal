package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ImmutableFacetValues;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

class CacheContainer {
  private final Map<VajramID, Map<ImmutableFacetValues, CompletableFuture<@Nullable Object>>>
      cache = new HashMap<>();

  void put(ImmutableFacetValues key, CompletableFuture<@Nullable Object> value) {
    cache.computeIfAbsent(key._vajramID(), k -> new HashMap<>()).put(key, value);
  }

  @Nullable CompletableFuture<@Nullable Object> get(ImmutableFacetValues key) {
    return cache.getOrDefault(key._vajramID(), Map.of()).get(key);
  }

  @SuppressWarnings("return")
  Collection<ImmutableFacetValues> getKeys(VajramID vajramID) {
    return cache.getOrDefault(vajramID, Map.of()).keySet();
  }
}
