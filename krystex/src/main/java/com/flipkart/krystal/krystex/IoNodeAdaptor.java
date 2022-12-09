package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface IoNodeAdaptor<T> {

  Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> adaptLogic(
      Function<
              ImmutableList<ImmutableMap<String, ?>>,
              ImmutableMap<ImmutableMap<String, ?>, CompletableFuture<T>>>
          logicToDecorate);

  static <T> IoNodeAdaptor<T> noop() {
    //noinspection unchecked
    return (IoNodeAdaptor<T>) NoopIoNodeAdaptor.INSTANCE;
  }
}
