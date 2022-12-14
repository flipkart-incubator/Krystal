package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;

public class TypeUtils {

  public static Optional<Type> getJavaType(DataType dataType) {
    if (dataType instanceof JavaDataType<?> javaDataType) {
      return javaDataType.javaType();
    } else {
      throw new IllegalArgumentException("Unknown datatype: %s".formatted(dataType));
    }
  }

  private TypeUtils() {}
}
