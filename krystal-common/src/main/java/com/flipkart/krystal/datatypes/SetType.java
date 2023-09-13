package com.flipkart.krystal.datatypes;

import static com.flipkart.krystal.datatypes.TypeUtils.getJavaType;

import com.google.common.primitives.Primitives;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SetType<T> implements DataType<LinkedHashSet<T>> {
  private final DataType<?> type;

  public static <T> SetType<T> set(DataType<?> type) {
    return new SetType<>(type);
  }

  @Override
  public Optional<Type> javaType() {
    return type.javaType()
        .map(
            t -> {
              if (t instanceof Class<?> clazz) {
                return Primitives.wrap(clazz);
              } else {
                return t;
              }
            })
        .map(t -> getJavaType(LinkedHashSet.class, t));
  }
}
