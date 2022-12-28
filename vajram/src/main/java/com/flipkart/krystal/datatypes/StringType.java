package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;

public final class StringType extends AbstractDataType<String> {

  private static final StringType INSTANCE = new StringType();

  public static StringType string() {
    return INSTANCE;
  }

  private StringType() {}

  @Override
  Type javaType() {
    return String.class;
  }
}
