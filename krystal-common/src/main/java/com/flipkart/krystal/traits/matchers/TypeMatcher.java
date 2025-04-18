package com.flipkart.krystal.traits.matchers;

import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class TypeMatcher implements InputValueMatcher {

  private final @NonNull Class<?> type;

  public TypeMatcher(@NonNull Class<?> type) {
    this.type = type;
  }

  @Override
  public boolean matches(@Nullable Object inputValue) {
    return type.isInstance(inputValue);
  }
}
