package com.flipkart.krystal.data;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

@EqualsAndHashCode
@ToString
final class Nil<T> implements Errable<T> {
  static final Nil<Object> NIL = new Nil<>();

  private final CompletableFuture<@Nullable T> NIL_FUTURE = completedFuture(null);

  static <T> Nil<T> nil() {
    //noinspection unchecked
    return (Nil<T>) NIL;
  }

  @Override
  public CompletableFuture<@Nullable T> toFuture() {
    return NIL_FUTURE;
  }

  @Override
  public Optional<T> valueOpt() {
    return Optional.empty();
  }

  @Override
  public Optional<T> valueOptOrThrow() {
    return Optional.empty();
  }

  @Override
  public T valueOrThrow() {
    return valueOpt().orElseThrow();
  }

  Nil() {}
}
