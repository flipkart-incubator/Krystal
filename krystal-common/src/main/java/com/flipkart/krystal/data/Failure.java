package com.flipkart.krystal.data;

import static java.util.concurrent.CompletableFuture.failedFuture;

import com.flipkart.krystal.except.KrystalCompletionException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record Failure<T>(Throwable error) implements Errable<T> {

  @Override
  public CompletableFuture<@Nullable T> toFuture() {
    return failedFuture(error);
  }

  @Override
  public void completeFuture(CompletableFuture<T> future) {
    future.completeExceptionally(error);
  }

  @Override
  public @Nullable T value() {
    return null;
  }

  @Override
  public Optional<@NonNull T> valueOptOrThrow() {
    throw asRuntimeException();
  }

  @Override
  public @NonNull T valueOrThrow() {
    throw asRuntimeException();
  }

  @Override
  public void handle(Consumer<Failure<T>> ifFailure, Runnable ifNil, Consumer<T> ifNonNil) {
    ifFailure.accept(this);
  }

  @Override
  public <U> U mapToValue(
      Function<Failure<T>, U> ifFailure, Supplier<U> ifNil, Function<T, U> ifNonNil) {
    return ifFailure.apply(this);
  }

  @Override
  public <U> Errable<U> map(Function<T, U> ifNonNil) {
    return cast();
  }

  @Override
  public Optional<@NonNull T> valueOpt() {
    return Optional.empty();
  }

  @Override
  public String toString() {
    return error.toString();
  }

  @Override
  public Optional<Throwable> errorOpt() {
    return Optional.of(error);
  }

  private RuntimeException asRuntimeException() {
    return error instanceof RuntimeException e
        ? e
        : new KrystalCompletionException("Failure", error);
  }

  @SuppressWarnings("unchecked")
  public <U> Failure<U> cast() {
    return (Failure<U>) this;
  }
}
