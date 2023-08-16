package com.flipkart.krystal.datatypes;

import com.google.common.primitives.Primitives;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SetType<T> implements JavaDataType<LinkedHashSet<T>> {
  private final DataType type;

  public static <T> SetType<T> set(DataType type) {
    return new SetType<>(type);
  }

  @Override
  public Optional<Type> javaType() {
    if (type instanceof JavaDataType<?> javaDataType) {
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
                      return LinkedHashSet.class;
                    }

                    @Override
                    @Nullable
                    public Type getOwnerType() {
                      return null;
                    }
                  });
    } else {
      return Optional.empty();
    }
  }
}
