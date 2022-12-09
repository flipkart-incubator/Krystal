package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

final class NoopIoNodeAdaptor<T> implements IoNodeAdaptor<T> {

  static final NoopIoNodeAdaptor<?> INSTANCE = new NoopIoNodeAdaptor<>();

  @Override
  public Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> adaptLogic(
      Function<
              ImmutableList<ImmutableMap<String, ?>>,
              ImmutableMap<ImmutableMap<String, ?>, CompletableFuture<T>>>
          logicToDecorate) {
    return stringImmutableMap -> {
      ImmutableMap<ImmutableMap<String, ?>, CompletableFuture<T>> apply =
          logicToDecorate.apply(ImmutableList.of(stringImmutableMap));
      CompletableFuture<T> resultFuture = apply.get(stringImmutableMap);
      if (resultFuture == null) {
        throw new IllegalArgumentException("A future is mandatory");
      }
      return resultFuture.thenApply(ImmutableList::of);
    };
  }

  private NoopIoNodeAdaptor() {}
}
