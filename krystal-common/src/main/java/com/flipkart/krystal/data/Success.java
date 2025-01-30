package com.flipkart.krystal.data;

import java.util.Optional;

public abstract sealed class Success<T> implements Errable<T> permits Nil, NonNil {

  private static final Success NIL = Nil.nil();

  @Override
  public final T valueOrThrow() {
    return valueOpt().orElseThrow();
  }

  @Override
  public final Optional<T> valueOptOrThrow() {
    return valueOpt();
  }

  public final Optional<Throwable> errorOpt() {
    return Optional.empty();
  }

  static <T> Success<T> nil() {
    return NIL;
  }
}
