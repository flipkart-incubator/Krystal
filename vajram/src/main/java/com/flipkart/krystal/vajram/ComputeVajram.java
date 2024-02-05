package com.flipkart.krystal.vajram;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract non-sealed class ComputeVajram<T> extends AbstractVajram<T> {

  @Override
  public final ImmutableMap<Facets, CompletableFuture<@Nullable T>> execute(
      ImmutableList<Facets> facetsList) {
    try {
      return executeCompute(facetsList).entrySet().stream()
          .collect(toImmutableMap(Entry::getKey, ComputeVajram::toFuture));
    } catch (Throwable e) {
      return facetsList.stream().collect(toImmutableMap(identity(), i -> failedFuture(e)));
    }
  }

  private static <T> CompletableFuture<@Nullable T> toFuture(Entry<Facets, Errable<T>> e) {
    return e.getValue().toFuture();
  }

  public abstract ImmutableMap<Facets, Errable<T>> executeCompute(ImmutableList<Facets> facetsList);
}
