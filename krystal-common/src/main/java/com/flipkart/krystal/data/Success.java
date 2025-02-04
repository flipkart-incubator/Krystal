package com.flipkart.krystal.data;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

public sealed interface Success<T> extends Errable<@NonNull T> permits Nil, NonNil {

  @Override
  public default @NonNull T valueOrThrow() {
    @SuppressWarnings("method.invocation")
    T t = valueOpt().orElseThrow();
    return t;
  }

  @Override
  public default Optional<@NonNull T> valueOptOrThrow() {
    return valueOpt();
  }

  @Override
  public default Optional<Throwable> errorOpt() {
    return Optional.empty();
  }

  static <T> Success<T> nil() {
    return Nil.nil();
  }
}
