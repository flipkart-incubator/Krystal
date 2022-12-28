package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaType<T> extends AbstractDataType<T> {

  private final Class<T> clazz;

  public static <T> JavaType<T> java(Class<T> clazz) {
    return new JavaType<>(clazz);
  }

  @Override
  Type javaType() {
    return clazz;
  }
}
