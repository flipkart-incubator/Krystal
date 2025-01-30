package com.flipkart.krystal.data;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@EqualsAndHashCode(of = "value")
@ToString(of = "value")
public final class NonNil<T> extends Success<T> {
  @Getter private final @NonNull T value;

  private @MonotonicNonNull CompletableFuture<@Nullable T> c;
  private Optional<T> o = Optional.empty();

  public NonNil(@NonNull T value) {
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
}
