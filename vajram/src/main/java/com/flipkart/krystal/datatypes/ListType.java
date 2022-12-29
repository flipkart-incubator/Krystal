package com.flipkart.krystal.datatypes;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ListType<T> extends AbstractDataType<ArrayList<T>> {
  private final DataType<T> type;

  public static <T> ListType<T> list(DataType<T> type) {
    return new ListType<>(type);
  }

  @Override
  Type javaType() {
    return new TypeToken<ArrayList<T>>() {}.getType();
  }
}
