package com.flipkart.krystal.traits.matchers;

import static com.flipkart.krystal.traits.matchers.AnythingMatcher.ANY_NON_NULL;
import static com.flipkart.krystal.traits.matchers.AnythingMatcher.ANY_VALUE;

import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
public sealed interface InputValueMatcher<T>
    permits AnythingMatcher, EnumMatcher, StringMatcher, TypeMatcher {
  boolean matches(@Nullable T inputValue);

  static <T> InputValueMatcher<T> isInstanceOf(Class<? extends T> type) {
    return new TypeMatcher<>(type);
  }

  static <T extends Enum<T>> InputValueMatcher<T> equalsEnum(@NonNull Enum<T> enumValue) {
    return new EnumMatcher<>(enumValue);
  }

  @SuppressWarnings("unchecked")
  static <T> InputValueMatcher<T> isAnyValue() {
    return (InputValueMatcher<T>) ANY_VALUE;
  }

  @SuppressWarnings("unchecked")
  static <T> InputValueMatcher<T> isAnyNonNullValue() {
    return (InputValueMatcher<T>) ANY_NON_NULL;
  }
}
