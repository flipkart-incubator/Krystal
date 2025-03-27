package com.flipkart.krystal.data;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;

public sealed interface Success<T> extends Errable<@NonNull T> permits Nil, NonNil {

  @Override
  default Optional<@NonNull T> valueOptOrThrow() {
    return valueOpt();
  }

  @Override
  default Optional<Throwable> errorOpt() {
    return Optional.empty();
  }

  static <T> Success<T> nil() {
    return Nil.nil();
  }
}
