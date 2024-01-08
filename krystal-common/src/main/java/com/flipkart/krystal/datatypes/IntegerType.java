package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;

@SuppressWarnings("Singleton")
public final class IntegerType implements DataType<Integer> {

  private static final IntegerType INSTANCE = new IntegerType();

  public static IntegerType integer() {
    return INSTANCE;
  }

  private IntegerType() {}

  @Override
  public Optional<Type> javaType() {
    return Optional.of(Integer.TYPE);
  }
}
