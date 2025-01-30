package com.flipkart.krystal.data;

import static java.util.concurrent.CompletableFuture.failedFuture;

import com.flipkart.krystal.except.StackTracelessException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.EqualsAndHashCode;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@EqualsAndHashCode(of = "error")
public final class Failure<T> implements Errable<T> {
  private final Throwable error;

  private @MonotonicNonNull CompletableFuture<@Nullable T> c;
  private Optional<Throwable> o = Optional.empty();

  public Failure(Throwable error) {
    this.error = error;
  }

  @Override
  public CompletableFuture<@Nullable T> toFuture() {
    return c != null ? c : (c = failedFuture(error));
  }

  @Override
  public Optional<T> valueOptOrThrow() {
    throw asRuntimException();
  }

  @Override
  public T valueOrThrow() {
    throw asRuntimException();
  }

  @Override
  public Optional<T> valueOpt() {
    return Optional.empty();
  }

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

  private RuntimeException asRuntimException() {
    return error instanceof RuntimeException e ? e : new StackTracelessException("Failure", error);
  }
}
