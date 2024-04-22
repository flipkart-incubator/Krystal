package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Request;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

@FunctionalInterface
public non-sealed interface OutputLogic<T> extends Logic {
  ImmutableMap<Facets, CompletableFuture<@Nullable T>> execute(
      ImmutableList<? extends Facets> inputs);
}
