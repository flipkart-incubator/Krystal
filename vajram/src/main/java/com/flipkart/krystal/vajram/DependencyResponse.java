package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.data.ValueOrError.empty;

import com.flipkart.krystal.data.ValueOrError;
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

  public V getOnlyValueOrThrow() {
    ValueOrError<V> value = getOnlyValue();
    if (value.error().isPresent()) {
      throw new IllegalStateException("Received an error.", value.error().get());
    }
    if (value.value().isEmpty()) {
      throw new IllegalStateException("Received empty response.");
    }
    return value.value().get();
  }

  private ValueOrError<V> getOnlyValue() {
    ImmutableCollection<ValueOrError<V>> values = values();
    if (values.size() != 1) {
      throw new IllegalStateException(
          "Expected to find 1 dependency response, found %s".formatted(values.size()));
    }
    return values.iterator().next();
  }

  public ImmutableCollection<ValueOrError<V>> values() {
    return responses.values();
  }
}
