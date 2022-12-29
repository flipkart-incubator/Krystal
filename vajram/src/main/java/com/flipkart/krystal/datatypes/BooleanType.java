package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;

public final class BooleanType extends AbstractDataType<Boolean> {
  private static final BooleanType INSTANCE = new BooleanType();

  public static BooleanType bool() {
    return INSTANCE;
  }

  private BooleanType() {}

  @Override
  Type javaType() {
    return Boolean.class;
  }
}
