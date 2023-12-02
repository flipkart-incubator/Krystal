package com.flipkart.krystal.vajram;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.futures.DelayableFuture;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract non-sealed class DelayableVajram<P, R> extends AbstractVajram<R> {
  public abstract ImmutableMap<Inputs, DelayableFuture<@Nullable P, @Nullable R>> execute(
      ImmutableList<Inputs> inputs);
}
