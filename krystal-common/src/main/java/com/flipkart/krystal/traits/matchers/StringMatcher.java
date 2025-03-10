package com.flipkart.krystal.traits.matchers;

import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class StringMatcher implements InputValueMatcher {

  @NonNull private final String stringValue;

  public StringMatcher(@NonNull String stringValue) {
    this.stringValue = stringValue;
  }

  @Override
  public boolean matches(@Nullable Object inputValue) {
    return stringValue.equals(inputValue);
  }
}
