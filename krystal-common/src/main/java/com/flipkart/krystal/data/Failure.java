package com.flipkart.krystal.data;

import static java.util.concurrent.CompletableFuture.failedFuture;

import com.flipkart.krystal.except.StackTracelessException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@EqualsAndHashCode(of = "error")
public final class Failure<T> implements Errable<T> {
  private final Throwable error;

  @SuppressWarnings({"optional.field", "OptionalUsedAsFieldOrParameterType"})
  private Optional<Throwable> o = Optional.empty();

  public Failure(Throwable error) {
    this.error = error;
  }

  @Override
  public CompletableFuture<@Nullable T> toFuture() {
    return failedFuture(error);
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
  public void handle(Consumer<Failure<T>> ifFailure, Runnable ifNil, Consumer<NonNil<T>> ifNonNil) {
    ifFailure.accept(this);
  }

  @Override
  public <U> U map(
      Function<Failure<T>, U> ifFailure, Supplier<U> ifNil, Function<NonNil<T>, U> ifNonNil) {
    return ifFailure.apply(this);
  }

  @Override
  public Optional<@NonNull T> valueOpt() {
    return Optional.empty();
  }

  @Override
  public String toString() {
    return error.toString();
  }

  public Throwable error() {
    return error;
  }

  @Override
  public Optional<Throwable> errorOpt() {
    return o.isPresent() ? o : (o = Optional.of(error));
  }

  private RuntimeException asRuntimeException() {
    return error instanceof RuntimeException e ? e : new StackTracelessException("Failure", error);
  }

  @SuppressWarnings("unchecked")
  public <U> Failure<U> cast() {
    return (Failure<U>) this;
  }
}
