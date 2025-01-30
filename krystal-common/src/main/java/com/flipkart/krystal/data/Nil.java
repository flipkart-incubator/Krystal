package com.flipkart.krystal.data;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("Singleton")
@EqualsAndHashCode
@ToString
final class Nil<T> extends Success<T> {
  private static final Nil NIL = new Nil();
  private static final CompletableFuture NIL_FUTURE = completedFuture(null);

  @Override
  public CompletableFuture<@Nullable T> toFuture() {
    return NIL_FUTURE;
  }

  @Override
  public Optional<T> valueOpt() {
    return Optional.empty();
  }

  static <T> Nil<T> nil() {
    return NIL;
  }

  private Nil() {}
}
