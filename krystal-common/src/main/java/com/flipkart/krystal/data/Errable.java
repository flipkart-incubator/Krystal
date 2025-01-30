package com.flipkart.krystal.data;

import com.flipkart.krystal.except.StackTracelessException;
import com.flipkart.krystal.except.ThrowingCallable;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface Errable<@NonNull T> extends FacetValue<T> permits Success, Failure {

  /**
   * @return a {@link CompletableFuture} which is completed exceptionally with the error if this is
   *     a {@link Failure}, or completed normally with the value if this is a {@link Success}, or
   *     completed normally with null if this is {@link Nil}
   */
  CompletableFuture<@Nullable T> toFuture();

  Optional<T> valueOpt();

  Optional<Throwable> errorOpt();

  /**
   * @return a non-empty {@link Optional} if this is a {@link Success} or empty {@link Optional} it
   *     this is {@link Nil}
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

  /************************************************************************************************/
  /*************************************** Static utilities ***************************************/
  /************************************************************************************************/

  static <T> Errable<T> of(Errable<T> t) {
    return t;
  }

  static <T> Errable<T> of(Optional<T> t) {
    return withValue(t.orElse(null));
  }

  static <T> Errable<T> of(@Nullable Object t) {
    if (t instanceof Errable<?>) {
      if (t instanceof NonNil<?> success) {
        return of((T) success.value());
      } else {
        return (Errable<T>) t;
      }
    } else if (t instanceof Optional<?> o) {
      return of(((Optional<T>) o).orElse(null));
    } else {
      return withValue((T) t);
    }
  }

  static <T> Errable<T> nil() {
    return Success.nil();
  }

  static <T> Errable<T> withValue(@Nullable T t) {
    return t != null ? new NonNil<>(t) : nil();
  }

  static <T> Errable<T> withError(Throwable t) {
    return new Failure<>(t);
  }

  static <T> Errable<T> errableFrom(ThrowingCallable<@Nullable T> valueProvider) {
    try {
      return withValue(valueProvider.call());
    } catch (Throwable e) {
      return withError(e);
    }
  }

  static <S, T> Function<S, Errable<T>> computeErrableFrom(Function<S, @Nullable T> valueComputer) {
    return s -> errableFrom(() -> valueComputer.apply(s));
  }

  @SuppressWarnings("unchecked")
  static <T> Errable<T> errableFrom(@Nullable Object value, @Nullable Throwable error) {
    if (value instanceof Optional<?> valueOpt) {
      if (valueOpt.isPresent()) {
        if (error != null) {
          throw illegalState();
        } else {
          return errableFrom((T) valueOpt.get(), null);
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
}
