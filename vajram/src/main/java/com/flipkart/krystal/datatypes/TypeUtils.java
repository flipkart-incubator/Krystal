package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;

public class TypeUtils {

  public static Type getJavaType(DataType<?> dataType) {
    if (dataType instanceof AbstractDataType<?> abstractDataType) {
      return abstractDataType.javaType();
    } else {
      throw new IllegalArgumentException("Unknown datatype: %s".formatted(dataType));
    }
  }

  private TypeUtils() {}
}
