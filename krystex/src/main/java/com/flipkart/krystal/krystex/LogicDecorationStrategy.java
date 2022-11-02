package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface LogicDecorationStrategy {
  <T> Function<ImmutableMap<String, ?>, CompletableFuture<T>> decorateLogic(
      Node<T> node, Function<ImmutableMap<String, ?>, CompletableFuture<T>> logicToDecorate);
}
