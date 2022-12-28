package com.flipkart.krystal.datatypes;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SetType<T> extends AbstractDataType<LinkedHashSet<T>> {
  private final DataType<T> type;

  public static <T> SetType<T> set(DataType<T> type) {
    return new SetType<>(type);
  }

  @Override
  Type javaType() {
    return new TypeToken<LinkedHashSet<T>>() {}.getType();
  }
}
