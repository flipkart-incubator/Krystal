package com.flipkart.krystal.datatypes;

public final class IntegerType implements DataType<Integer> {

  private static final IntegerType INSTANCE = new IntegerType();

  public static IntegerType integer() {
    return INSTANCE;
  }

  private IntegerType() {}
}
