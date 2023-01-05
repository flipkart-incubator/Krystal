package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;

public final class BooleanType implements JavaDataType<Boolean>, DataType {
  private static final BooleanType INSTANCE = new BooleanType();

  public static BooleanType bool() {
    return INSTANCE;
  }

  private BooleanType() {}

  @Override
  public Optional<Type> javaType() {
    return Optional.of(Boolean.class);
  }
}
