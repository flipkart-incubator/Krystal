package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;

public final class StringType implements JavaDataType<String> {

  private static final StringType INSTANCE = new StringType();

  public static StringType string() {
    return INSTANCE;
  }

  private StringType() {}

  @Override
  public Optional<Type> javaType() {
    return Optional.of(String.class);
  }
}
