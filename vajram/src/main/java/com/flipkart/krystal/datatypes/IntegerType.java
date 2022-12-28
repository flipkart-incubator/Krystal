package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;

public final class IntegerType extends AbstractDataType<Integer> {

  private static final IntegerType INSTANCE = new IntegerType();

  public static IntegerType integer() {
    return INSTANCE;
  }

  private IntegerType() {}

  @Override
  Type javaType() {
    return Integer.TYPE;
  }
}
