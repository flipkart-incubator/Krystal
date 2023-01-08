package com.flipkart.krystal.datatypes;

import com.google.common.primitives.Primitives;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ListType<T> implements JavaDataType<ArrayList<T>> {
  private final DataType typeParam;

  public static <T> ListType<T> list(DataType type) {
    return new ListType<>(type);
  }

  @Override
  public Optional<Type> javaType() {
    if (typeParam instanceof JavaDataType<?> javaDataType) {
      return javaDataType
          .javaType()
          .map(
              t -> {
                if (t instanceof Class<?> clazz) {
                  return Primitives.wrap(clazz);
                } else {
                  return t;
                }
              })
          .map(
              t ->
                  new ParameterizedType() {
                    @Override
                    public Type[] getActualTypeArguments() {
                      return new Type[] {t};
                    }

                    @Override
                    public Type getRawType() {
                      return ArrayList.class;
                    }

                    @Override
                    public Type getOwnerType() {
                      return null;
                    }
                  });
    } else {
      return Optional.empty();
    }
  }
}
