package com.flipkart.krystal.datatypes;

import static com.flipkart.krystal.datatypes.TypeUtils.getJavaType;

import com.google.common.primitives.Primitives;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ListType<T> implements DataType<ArrayList<T>> {

  public static final Class<ArrayList> COLLECTION_TYPE = ArrayList.class;
  private final DataType<?> typeParam;

  public static <T> ListType<T> list(DataType<?> type) {
    return new ListType<>(type);
  }

  @Override
  public Optional<Type> javaType() {
    return typeParam
        .javaType()
        .map(
            t -> {
              if (t instanceof Class<?> clazz) {
                return Primitives.wrap(clazz);
              } else {
                return t;
              }
            })
        .map(t -> getJavaType(COLLECTION_TYPE, t));
  }
}
