package com.flipkart.krystal.data;

import static com.flipkart.krystal.data.Nil.nil;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("ClassReferencesSubclass") // By design
public sealed interface Errable<@NonNull T> extends FacetValue<T> permits Success, Failure, Nil {

  static <T> Nil<T> empty() {
    return nil();
  }

  static <T> Errable<T> withValue(@Nullable T t) {
    return t != null ? new Success<>(t) : empty();
  }

  static <T> Errable<T> withError(Throwable t) {
    return new Failure<>(t);
  }

  static <T> Errable<T> errableFrom(Callable<T> valueProvider) {
    try {
      return withValue(valueProvider.call());
    } catch (Throwable e) {
      return withError(e);
    }
  }

  static <S, T> Function<S, Errable<T>> errableFrom(Function<S, T> valueComputer) {
    return s -> errableFrom(() -> valueComputer.apply(s));
  }

  @SuppressWarnings("unchecked")
  static <T> Errable<T> errableFrom(@Nullable Object value, @Nullable Throwable error) {
    if (value instanceof Optional<?> valueOpt) {
      if (valueOpt.isPresent()) {
        if (error != null) {
          throw illegalState();
        } else {
          return withValue((T) valueOpt.get());
        }
      } else if (error != null) {
        return withError(error);
      } else {
        return nil();
      }
    } else if (value != null) {
      if (error != null) {
        throw illegalState();
      } else {
        return (Errable<T>) withValue(value);
      }
    } else if (error != null) {
      return withError(error);
    } else {
      return nil();
    }
  }

  private static IllegalArgumentException illegalState() {
    return new IllegalArgumentException("Both of 'value' and 'error' cannot be present together");
  }

  /**
   * @return a {@link CompletableFuture} which is completed exceptionally with the error if this is
   *     a {@link Failure}, or completed normally with the value if this is a {@link Success}, or
   *     completed normally with null if this is {@link Nil}
   */
  CompletableFuture<@Nullable T> toFuture();

  Optional<T> valueOpt();

  /**
   * @return a non-empty {@link Optional} if this is a {@link Success} or empty {@link Optional} it
   *     this is {@link Nil}
   * @throws RuntimeException if this is a {@link Failure}. If the error in {@link Failure} is not a
   *     {@link RuntimeException}, then the error is wrapped in a new {@link RuntimeException} and
   *     thrown.
   */
  Optional<T> valueOptOrThrow();

  T valueOrThrow();

  default Optional<Throwable> errorOpt() {
    return Optional.empty();
  }
}
