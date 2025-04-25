package com.flipkart.krystal.traits.matchers;

import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class EnumMatcher<T extends Enum<T>> implements InputValueMatcher<T> {

  private final Enum<T> enumValue;

  public EnumMatcher(@NonNull Enum<T> enumValue) {
    this.enumValue = enumValue;
  }

  @Override
  public boolean matches(@Nullable T inputValue) {
    return enumValue == inputValue;
  }
}
