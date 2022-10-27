package com.flipkart.krystal.datatypes;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ListType<T> implements DataType<List<T>> {
  private final DataType<T> type;

  public static <T> ListType<T> list(DataType<T> type) {
    return new ListType<>(type);
  }
}
