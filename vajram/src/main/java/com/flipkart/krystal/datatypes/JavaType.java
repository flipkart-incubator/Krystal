package com.flipkart.krystal.datatypes;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class JavaType<T> implements DataType<T> {

  private final Class<T> clazz;

  public static <T> JavaType<T> java(Class<T> clazz) {
    return new JavaType<>(clazz);
  }
}
