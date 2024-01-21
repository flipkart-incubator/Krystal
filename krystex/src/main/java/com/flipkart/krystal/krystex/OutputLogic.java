package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

@FunctionalInterface
public non-sealed interface OutputLogic<T> extends Logic {
  ImmutableMap<Inputs, CompletableFuture<@Nullable T>> execute(ImmutableList<Inputs> inputs);
}
