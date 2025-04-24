package com.flipkart.krystal.data;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@EqualsAndHashCode(of = "value")
@ToString(of = "value")
public final class NonNil<T> implements Success<T> {
  private final @NonNull T value;

  private @MonotonicNonNull CompletableFuture<@Nullable T> c;

  @SuppressWarnings({"optional.field", "OptionalUsedAsFieldOrParameterType"})
  private Optional<@NonNull T> o = Optional.empty();

  public NonNil(@NonNull T value) {
    this.value = value;
  }

  @Override
  public CompletableFuture<@Nullable T> toFuture() {
    return c != null ? c : (c = completedFuture(value));
  }

  @Override
  public Optional<@NonNull T> valueOpt() {
    return o.isPresent() ? o : (o = Optional.of(value));
  }

  @Override
  public @NonNull T valueOrThrow() {
    return value;
  }

  @Override
  public @NonNull T value() {
    return value;
  }
}
