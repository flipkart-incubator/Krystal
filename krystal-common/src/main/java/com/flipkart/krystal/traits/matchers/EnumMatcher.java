package com.flipkart.krystal.traits.matchers;

import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class EnumMatcher implements InputValueMatcher {

  private final Enum<?> enumValue;

  public EnumMatcher(@NonNull Enum<?> enumValue) {
    this.enumValue = enumValue;
  }

  @Override
  public boolean matches(@Nullable Object inputValue) {
    return enumValue == inputValue;
  }
}
