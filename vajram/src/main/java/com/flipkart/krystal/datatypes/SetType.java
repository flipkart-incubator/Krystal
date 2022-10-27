package com.flipkart.krystal.datatypes;

import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SetType<T> implements DataType<Set<T>> {
  private final DataType<T> type;

  public static <T> SetType<T> set(DataType<T> type) {
    return new SetType<>(type);
  }
}
