package com.flipkart.krystal.traits.matchers;

import org.checkerframework.checker.nullness.qual.Nullable;

final class TypeMatcher<T> implements InputValueMatcher<T> {

  private final Class<? extends T> type;

  public TypeMatcher(Class<? extends T> type) {
    this.type = type;
  }

  @Override
  public boolean matches(@Nullable Object inputValue) {
    return type.isInstance(inputValue);
  }
}
