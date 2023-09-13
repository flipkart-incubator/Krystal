package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;

@SuppressWarnings("Singleton")
public final class BooleanType implements DataType<Boolean> {
  private static final BooleanType INSTANCE = new BooleanType();

  public static BooleanType bool() {
    return INSTANCE;
  }

  private BooleanType() {}

  @Override
  public Optional<Type> javaType() {
    return Optional.of(Boolean.TYPE);
  }
}
