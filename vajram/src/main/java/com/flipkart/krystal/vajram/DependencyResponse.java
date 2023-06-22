package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.data.ValueOrError.empty;

import com.flipkart.krystal.data.ValueOrError;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Supplier;

public record DependencyResponse<R extends VajramRequest, V>(
    ImmutableMap<R, ValueOrError<V>> responses) {
  public ValueOrError<V> get(R request) {
    return responses.getOrDefault(request, empty());
  }

  public V getOrThrow(R request, Supplier<Exception> exceptionSupplier) throws Exception {
    return responses.getOrDefault(request, empty()).value().orElseThrow(exceptionSupplier);
  }

  /**
   * This method will be removed in a future version. This method hides the actual exception or
   * cause of a missing value. Please use {@link #getOnlyValue()}.orElseThrow(() -> ...) instead so
   * that explicit exceptions can be thrown.
   *
   * @deprecated Use {@link #getOnlyValue()}.orElseThrow() instead.
   */
  @Deprecated(forRemoval = true)
  public V getOnlyValueOrThrow() {
    return getOnlyValue().orElseThrow(() -> new IllegalStateException("Received empty response."));
  }

  public Optional<V> getOnlyValue() {
    ValueOrError<V> value = getOnlyValueOrError();
    if (value.error().isPresent()) {
      throw new IllegalStateException("Received an error.", value.error().get());
    }
    Optional<V> opt = value.value();
    return opt;
  }

  private ValueOrError<V> getOnlyValueOrError() {
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
