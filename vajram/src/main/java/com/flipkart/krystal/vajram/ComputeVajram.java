package com.flipkart.krystal.vajram;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Compute vajrams are vajrams whose output logic can be executed in the calling thread itself . In
 * other words, the output logic does not delegate outside the calling thread.
 *
 * <p>This means that ComputeVajrams cannot make network calls to compute the output value. They
 * cannot submit the computation task to an {@link ExecutorService}, nor can they make any other IO
 * calls like disk look ups or even calls to other processes to compute the output.
 *
 * @param <T> The type of the output of this vajram.
 */
public abstract non-sealed class ComputeVajram<T> extends AbstractVajram<T> {

  @Override
  public final ImmutableMap<Facets, CompletableFuture<@Nullable T>> execute(
      ImmutableList<Facets> facetsList) {
    return executeCompute(facetsList).entrySet().stream()
        .collect(toImmutableMap(Entry::getKey, ComputeVajram::toFuture));
  }

  private static <T> CompletableFuture<@Nullable T> toFuture(Entry<Facets, Errable<T>> e) {
    return e.getValue().toFuture();
  }

  public abstract ImmutableMap<Facets, Errable<T>> executeCompute(ImmutableList<Facets> facetsList);
}
