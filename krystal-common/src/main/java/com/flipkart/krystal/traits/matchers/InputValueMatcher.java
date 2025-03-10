package com.flipkart.krystal.traits.matchers;

import static com.flipkart.krystal.traits.matchers.AnythingMatcher.ANY_NON_NULL;
import static com.flipkart.krystal.traits.matchers.AnythingMatcher.ANY_VALUE;

import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
public sealed interface InputValueMatcher
    permits AnythingMatcher, EnumMatcher, StringMatcher, TypeMatcher {
  abstract boolean matches(@Nullable Object inputValue);

  static InputValueMatcher isInstanceOf(Class<?> type) {
    return new TypeMatcher(type);
  }

  static InputValueMatcher equalsEnum(@NonNull Enum<?> enumValue) {
    return new EnumMatcher(enumValue);
  }

  static InputValueMatcher isAnyValue() {
    return ANY_VALUE;
  }

  static InputValueMatcher isAnyNonNullValue() {
    return ANY_NON_NULL;
  }
}
