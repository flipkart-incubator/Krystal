package com.flipkart.krystal.krystex;

import java.util.Optional;

public record SingleValue<T>(Optional<T> value, Optional<Throwable> failureReason)
    implements Value {

  private static final SingleValue<?> EMPTY = new SingleValue<>(Optional.empty(), Optional.empty());

  public SingleValue {
    if ((value.isPresent() && failureReason.isPresent())) {
      throw new IllegalArgumentException(
          "Both of 'value' and 'failureReason' cannot be present together");
    }
  }

  public SingleValue(T value, Throwable exception) {
    this(Optional.ofNullable(value), Optional.ofNullable(exception));
  }

  public SingleValue(T value) {
    this(Optional.of(value), Optional.empty());
  }

  public static <T> SingleValue<T> empty() {
    //noinspection unchecked
    return (SingleValue<T>) EMPTY;
  }

  public boolean isFailure() {
    return !isSuccessful();
  }

  public boolean isSuccessful() {
    return failureReason().isEmpty();
  }
}
