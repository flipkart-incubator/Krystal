package com.flipkart.krystal.data;

import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.except.ThrowingCallable;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface Errable<T> extends FacetValue<T> permits Success, Failure {

  /**
   * Returns a {@link CompletableFuture} which is completed exceptionally with the error if this is
   * a {@link Failure}, or completed normally with the value if this is a {@link Success}, or
   * completed normally with null if this is {@link Nil}
   */
  CompletableFuture<@Nullable T> toFuture();

  /**
   * Returns an {@link Optional} which is has the value inside this Errable. The returned Optional
   * is present only if and only if this Errable is of the type {@link NonNil}. In all other cases,
   * the returned Optional is empty.
   */
  Optional<T> valueOpt();

  /**
   * Returns an {@link Optional} which is has the error which caused this Errable to be a {@link
   * Failure}. The returned Optional is present only if and only if this Errable is of the type
   * {@link Failure}. In all other cases, the returned Optional is empty.
   */
  Optional<Throwable> errorOpt();

  /**
   * Returns a non-empty {@link Optional} if this is a {@link Success} or empty {@link Optional} it
   * this is {@link Nil}
   *
   * @throws RuntimeException if this is a {@link Failure}. If the error in {@link Failure} is not a
   *     {@link RuntimeException}, then the error is wrapped in a new {@link RuntimeException} and
   *     thrown.
   */
  Optional<T> valueOptOrThrow();

  /**
   *
   *
   * <ul>
   *   <li>NonNil: returns the value <br>
   *   <li>Nil: throws {@link NoSuchElementException} <br>
   *   <li>Failure: throws a {@link RuntimeException} representing the throwable which caused the
   *       failure. If the throwable is a {@link RuntimeException}, it is thrown as is. Else it is
   *       wrapped in a {@link StackTracelessException} and thrown.
   * </ul>
   */
  T valueOrThrow();

  /* ***********************************************************************************************/
  /* ************************************** Static utilities ***************************************/
  /* ***********************************************************************************************/

  static <T> Errable<@NonNull T> of(Errable<@NonNull T> t) {
    return t;
  }

  @SuppressWarnings({"optional.parameter", "OptionalUsedAsFieldOrParameterType"})
  static <T> Errable<@NonNull T> of(Optional<T> t) {
    return withValue(t.orElse(null));
  }

  @SuppressWarnings("unchecked")
  static <T> Errable<@NonNull T> of(@Nullable Object t) {
    if (t instanceof Errable<?>) {
      if (t instanceof NonNil<?> success) {
        return of(success.value());
      } else {
        return (Errable<@NonNull T>) t;
      }
    } else if (t instanceof Optional<?> o) {
      return of(((Optional<T>) o).orElse(null));
    } else {
      return withValue((T) t);
    }
  }

  static <T> Errable<@NonNull T> nil() {
    return Success.nil();
  }

  static <T> Errable<@NonNull T> withValue(@Nullable T t) {
    return t != null ? new NonNil<>(t) : nil();
  }

  static <T> Errable<@NonNull T> withError(Throwable t) {
    return new Failure<>(t);
  }

  static <T> Errable<@NonNull T> errableFrom(ThrowingCallable<@Nullable T> valueProvider) {
    try {
      return withValue(valueProvider.call());
    } catch (Throwable e) {
      return withError(e);
    }
  }

  static <S, T> Function<S, Errable<@NonNull T>> computeErrableFrom(
      Function<S, @Nullable T> valueComputer) {
    return s -> errableFrom(() -> valueComputer.apply(s));
  }

  @SuppressWarnings("unchecked")
  static <T> Errable<@NonNull T> errableFrom(@Nullable Object value, @Nullable Throwable error) {
    if (value instanceof Optional<?> valueOpt) {
      if (valueOpt.isPresent()) {
        if (error != null) {
          throw illegalState();
        } else {
          return errableFrom(valueOpt.get(), null);
        }
      } else if (error != null) {
        return withError(error);
      } else {
        return Nil.nil();
      }
    } else if (value != null) {
      if (error != null) {
        throw illegalState();
      } else {
        return (Errable<@NonNull T>) withValue(value);
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
}
