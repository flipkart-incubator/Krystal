package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface MainLogic<T> {
  ImmutableMap<Inputs, CompletableFuture<T>> execute(ImmutableList<Inputs> inputs);
}
