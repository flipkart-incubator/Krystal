package com.flipkart.krystal.traits.matchers;

import org.checkerframework.checker.nullness.qual.Nullable;

final class AnythingMatcher implements InputValueMatcher {

  static final AnythingMatcher ANY_VALUE = new AnythingMatcher(true);
  static final AnythingMatcher ANY_NON_NULL = new AnythingMatcher(false);
  private final boolean allowNull;

  private AnythingMatcher(boolean allowNull) {
    this.allowNull = allowNull;
  }

  @Override
  public boolean matches(@Nullable Object inputValue) {
    return allowNull ? true : inputValue != null;
  }
}
