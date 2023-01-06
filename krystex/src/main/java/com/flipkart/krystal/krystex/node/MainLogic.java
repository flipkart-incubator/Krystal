package com.flipkart.krystal.krystex.node;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface MainLogic<T> {
  ImmutableMap<NodeInputs, CompletableFuture<T>> execute(ImmutableList<NodeInputs> inputs);
}
