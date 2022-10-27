package com.flipkart.krystal.datatypes;

public final class BooleanType implements DataType<Boolean> {
  private static final BooleanType INSTANCE = new BooleanType();

  public static BooleanType bool() {
    return INSTANCE;
  }

  private BooleanType() {}
}
