package com.flipkart.krystal.data;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A wrapper object representing a value or an error. This can be seen as a 'completed' version of a
 * {@link CompletableFuture}. This class avoids the prevalence of {@code null}s in the codebase and
 * also allows reactive frameworks to gracefully handle scenarios where a computation led to an
 * error because of which some value could not be computed.
 *
 * <p>Example states:
 *
 * <ul>
 *   <li>{@link #value()} is empty and {@link #error()} is empty - represents a null value.
 *   <li>{@link #value()} is empty and {@link #error()} is not empty - value could not be computed
 *       because of the given error.
 *   <li>{@link #value()} is not empty and {@link #error()} is empty - successfully computed the
 *       given value
 *   <li>{@link #value()} is not empty and {@link #error()} is not empty - this scenario is
 *       impossible.
 * </ul>
 */
public record ValueOrError<T>(Optional<T> value, Optional<Throwable> error)
    implements FacetValue<T> {

  private static final ValueOrError<?> EMPTY =
      new ValueOrError<>(Optional.empty(), Optional.empty());

  public ValueOrError {
    if ((value.isPresent() && error.isPresent())) {
      throw new IllegalArgumentException("Both of 'value' and 'error' cannot be present together");
    }
  }

  public static <T> ValueOrError<T> empty() {
    //noinspection unchecked
    return (ValueOrError<T>) EMPTY;
  }

  public static <T> ValueOrError<T> valueOrError(Callable<T> valueProvider) {
    try {
      return withValue(valueProvider.call());
    } catch (Throwable e) {
      return withError(e);
    }
  }

  public static <S, T> Function<S, ValueOrError<T>> valueOrError(Function<S, T> valueComputer) {
    return s -> valueOrError(() -> valueComputer.apply(s));
  }

  public static <T> ValueOrError<T> withValue(@Nullable T t) {
    return valueOrError(t, null);
  }

  public static <T> ValueOrError<T> withError(Throwable t) {
    return valueOrError(null, t);
  }

  public static <T> ValueOrError<T> valueOrError(
      @Nullable Object t, @Nullable Throwable throwable) {
    //noinspection unchecked,rawtypes
    return new ValueOrError<T>(
        (t instanceof Optional o) ? o : (Optional<T>) Optional.ofNullable(t),
        Optional.ofNullable(throwable));
  }

  /**
   * @return a new {@link CompletableFuture} which is completed exceptionally with the contents of
   *     {@link #error()} if it is present, or completed normally with contents of {@link #value()}
   *     (or null if it is empty)
   */
  public CompletableFuture<@Nullable T> toFuture() {
    if (error().isPresent()) {
      return CompletableFuture.failedFuture(error().get());
    } else {
      return CompletableFuture.completedFuture(value().orElse(null));
    }
  }

  public Optional<T> getValueOrThrow() {
    Optional<Throwable> error = error();
    if (error.isPresent()) {
      if (error.get() instanceof RuntimeException e) {
        throw e;
      } else {
        throw new RuntimeException(error.get());
      }
    }
    return value();
  }

  @Override
  public String toString() {
    if (error.isPresent()) {
      return error.toString();
    } else {
      return String.valueOf(value().orElse(null));
    }
  }
}
