package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.vajram.ValueOrError.empty;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import java.util.function.Supplier;

public record DependencyResponse<R extends VajramRequest, V>(
    ImmutableMap<R, ValueOrError<V>> responses) {
  public ValueOrError<V> get(R request) {
    return responses.getOrDefault(request, empty());
  }

  public V getOrThrow(R request, Supplier<Exception> exceptionSupplier) throws Exception {
    return responses.getOrDefault(request, empty()).value().orElseThrow(exceptionSupplier);
  }

  public ImmutableCollection<ValueOrError<V>> values() {
    return responses.values();
  }
}
