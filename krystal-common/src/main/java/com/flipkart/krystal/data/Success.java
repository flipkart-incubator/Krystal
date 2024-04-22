package com.flipkart.krystal.data;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.EqualsAndHashCode;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@EqualsAndHashCode(of = "value")
public final class Success<T> implements Errable<T> {
  private final @NonNull T value;

  private @MonotonicNonNull CompletableFuture<@Nullable T> c;
  private Optional<T> o = Optional.empty();

  public Success(@NonNull T value) {
    this.value = value;
  }

  @Override
  public CompletableFuture<@Nullable T> toFuture() {
    return c != null ? c : (c = completedFuture(value));
  }

  @Override
  public Optional<T> valueOpt() {
    return o.isPresent() ? o : (o = Optional.of(value));
  }

  @Override
  public Optional<T> valueOptOrThrow() {
    return valueOpt();
  }

  @Override
  public T valueOrThrow() {
    return valueOpt().orElseThrow();
  }

  public T value() {
    return value;
  }

  public String toString() {
    return value.toString();
  }
}
