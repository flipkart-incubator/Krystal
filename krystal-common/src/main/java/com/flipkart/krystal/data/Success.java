package com.flipkart.krystal.data;

import java.util.Optional;

public sealed interface Success<T> extends Errable<T> permits Nil, NonNil {

  @Override
  default Optional<T> valueOptOrThrow() {
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
