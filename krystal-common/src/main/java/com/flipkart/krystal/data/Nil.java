package com.flipkart.krystal.data;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("Singleton")
@EqualsAndHashCode
@ToString
public final class Nil<T> implements Success<T> {
  private static final Nil NIL = new Nil();

  @Override
  @SuppressWarnings("unchecked")
  public CompletableFuture<@Nullable T> toFuture() {
    return completedFuture(null);
  }

  @Override
  public @Nullable T value() {
    return null;
  }

  @Override
  public Optional<@NonNull T> valueOpt() {
    return Optional.empty();
  }

  @Override
  public @NonNull T valueOrThrow() {
    throw new NilValueException();
  }

  @Override
  public void handle(Consumer<Failure<T>> ifFailure, Runnable ifNil, Consumer<NonNil<T>> ifNonNil) {
    ifNil.run();
  }

  @SuppressWarnings("unchecked")
  static <T> Nil<T> nil() {
    return NIL;
  }

  private Nil() {}
}
