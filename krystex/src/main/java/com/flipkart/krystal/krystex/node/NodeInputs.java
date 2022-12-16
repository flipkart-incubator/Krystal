package com.flipkart.krystal.krystex.node;

import static com.flipkart.krystal.krystex.SingleValue.empty;

import com.flipkart.krystal.krystex.SingleValue;
import com.google.common.collect.ImmutableMap;
import java.util.NoSuchElementException;
import java.util.Optional;

public record NodeInputs(ImmutableMap<String, SingleValue<?>> values) {

  public NodeInputs() {
    this(ImmutableMap.of());
  }

  public SingleValue<?> getValue(String inputName) {
    return values().getOrDefault(inputName, empty());
  }

  public <T> Optional<T> get(String inputName) {
    //noinspection unchecked
    return Optional.ofNullable(this.values().get(inputName)).map(sv -> (T) sv.value().orElse(null));
  }

  public <T> T getOrThrow(String inputName) {
    SingleValue<?> singleValue = values().get(inputName);
    if (singleValue == null) {
      throw new NoSuchElementException();
    }
    //noinspection unchecked
    return (T)
        singleValue
            .value()
            .orElseThrow(
                () ->
                    new RuntimeException(
                        singleValue.failureReason().orElse(new NoSuchElementException())));
  }
}
