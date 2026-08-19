package com.flipkart.krystal.krystex.caching;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.google.common.collect.Iterators;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

class CacheContainer {
  // To be used as OutputLogicDecorator
  private final Map<VajramID, Map<ImmutableFacetValues, CompletableFuture<@Nullable Object>>>
      futureCache = new LinkedHashMap<>();

  // To be used as KryonLogicDecorator
  private final Map<VajramID, Map<ImmutableFacetValues, Errable<Object>>> valueCache =
      new LinkedHashMap<>();

  void putFuture(ImmutableFacetValues key, CompletableFuture<@Nullable Object> value) {
    futureCache.computeIfAbsent(key._vajramID(), k -> new HashMap<>()).put(key, value);
  }

  void putValue(ImmutableFacetValues key, Errable<Object> value) {
    valueCache.computeIfAbsent(key._vajramID(), k -> new HashMap<>()).put(key, value);
  }

  @Nullable CompletableFuture<@Nullable Object> getFuture(ImmutableFacetValues key) {
    return futureCache.getOrDefault(key._vajramID(), Map.of()).get(key);
  }

  @Nullable Errable<Object> getValue(ImmutableFacetValues key) {
    return valueCache.getOrDefault(key._vajramID(), Map.of()).get(key);
  }

  @SuppressWarnings("return")
  Iterator<ImmutableFacetValues> getKeys(VajramID vajramID) {
    return Iterators.concat(
        futureCache.getOrDefault(vajramID, Map.of()).keySet().iterator(),
        valueCache.getOrDefault(vajramID, Map.of()).keySet().iterator());
  }
}
