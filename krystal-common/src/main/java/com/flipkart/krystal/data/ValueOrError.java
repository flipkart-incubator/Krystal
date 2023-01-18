package com.flipkart.krystal.data;

import java.util.Optional;
import java.util.concurrent.Callable;

public record ValueOrError<T>(Optional<T> value, Optional<Throwable> error)
    implements InputValue<T> {

  private static final ValueOrError<?> EMPTY =
      new ValueOrError<>(Optional.empty(), Optional.empty());

  public ValueOrError {
    if ((value.isPresent() && error.isPresent())) {
      throw new IllegalArgumentException(
          "Both of 'value' and 'failureReason' cannot be present together");
    }
  }

  public static <T> ValueOrError<T> from(Callable<T> valueProvider) {
    try {
      return withValue(valueProvider.call());
    } catch (Exception e) {
      return new ValueOrError<>(Optional.empty(), Optional.of(e));
    }
  }

  public static <T> ValueOrError<T> empty() {
    //noinspection unchecked
    return (ValueOrError<T>) EMPTY;
  }

  public static <T> ValueOrError<T> withValue(T t) {
    return valueOrError(t, null);
  }

  public static <T> ValueOrError<T> error(Throwable t) {
    return valueOrError(null, t);
  }

  public static <T> ValueOrError<T> valueOrError(Object t, Throwable throwable) {
    //noinspection unchecked,rawtypes
    return new ValueOrError<T>(
        (t instanceof Optional o) ? o : (Optional<T>) Optional.ofNullable(t),
        Optional.ofNullable(throwable));
  }

  @Override
  public String toString() {
    if (error.isPresent()) {
      return error.toString();
    } else {
      return value().toString();
    }
  }
}
