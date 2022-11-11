package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface LogicDecorationStrategy {
  <T> Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> decorateLogic(
      Node<T> node, Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> logicToDecorate);
}
