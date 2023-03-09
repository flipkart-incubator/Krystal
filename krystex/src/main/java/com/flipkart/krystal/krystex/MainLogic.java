package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public non-sealed interface MainLogic<T> extends Logic {
  ImmutableMap<Inputs, CompletableFuture<T>> execute(ImmutableList<Inputs> inputs);
}
