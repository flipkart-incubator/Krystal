package com.flipkart.krystal.data;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record NonNil<T>(@NonNull T value) implements Success<T> {

  @Override
  public CompletableFuture<@Nullable T> toFuture() {
    return completedFuture(value);
  }

  @Override
  public Optional<@NonNull T> valueOpt() {
    return Optional.of(value);
  }

  @Override
  public @NonNull T valueOrThrow() {
    return value;
  }

  @Override
  public void handle(Consumer<Failure<T>> ifFailure, Runnable ifNil, Consumer<NonNil<T>> ifNonNil) {
    ifNonNil.accept(this);
  }
}
