package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.data.Errable.empty;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Failure;
import com.flipkart.krystal.data.ImmutableRequest;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public record DependencyResponse<R extends ImmutableRequest, V>(
    ImmutableMap<R, Errable<V>> responses) {
  public Errable<V> get(R request) {
    return responses.getOrDefault(request, empty());
  }

  public Optional<V> getOnlyValue() {
    Errable<V> value = getOnlyErrable();
    if (value instanceof Failure<V> f) {
      throw new IllegalStateException("Received an error.", f.error());
    }
    return value.valueOpt();
  }

  private Errable<V> getOnlyErrable() {
    ImmutableCollection<Errable<V>> values = values();
    if (values.size() != 1) {
      throw new IllegalStateException(
          "Expected to find 1 dependency response, found %s".formatted(values.size()));
    }
    return values.iterator().next();
  }

  public ImmutableCollection<Errable<V>> values() {
    return responses.values();
  }
}
