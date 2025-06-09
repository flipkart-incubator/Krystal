package com.flipkart.krystal.traits.matchers;

import org.checkerframework.checker.nullness.qual.Nullable;

final class StringMatcher implements InputValueMatcher {

  private final String stringValue;

  public StringMatcher(String stringValue) {
    this.stringValue = stringValue;
  }

  @Override
  public boolean matches(@Nullable Object inputValue) {
    return stringValue.equals(inputValue);
  }
}
