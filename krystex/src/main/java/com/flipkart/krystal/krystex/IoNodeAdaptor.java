package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface IoNodeAdaptor<T> {

  Function<NodeInputs, CompletableFuture<ImmutableList<T>>> adaptLogic(
      Function<ImmutableList<NodeInputs>, ImmutableMap<NodeInputs, CompletableFuture<T>>>
          logicToDecorate);

  static <T> IoNodeAdaptor<T> noop() {
    //noinspection unchecked
    return (IoNodeAdaptor<T>) NoopIoNodeAdaptor.INSTANCE;
  }
}
