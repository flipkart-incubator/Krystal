package com.flipkart.krystal.datatypes;

public final class StringType implements DataType<String> {

  private static final StringType INSTANCE = new StringType();

  public static StringType string() {
    return INSTANCE;
  }

  private StringType() {}
}
