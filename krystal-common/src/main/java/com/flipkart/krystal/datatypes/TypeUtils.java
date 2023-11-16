package com.flipkart.krystal.datatypes;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TypeUtils {

  private static final Map<String, Function<List<? extends DataType<?>>, DataType<?>>> mappings =
      new LinkedHashMap<>();

  static {
    mappings.put(boolean.class.getName(), _unused -> BooleanType.bool());
    mappings.put(Boolean.class.getName(), _unused -> BooleanType.bool());
    mappings.put(int.class.getName(), _unused -> IntegerType.integer());
    mappings.put(Integer.class.getName(), _unused -> IntegerType.integer());
    mappings.put(String.class.getName(), _unused -> StringType.string());
    mappings.put(List.class.getName(), typeParams -> ListType.list(typeParams.get(0)));
    mappings.put(Set.class.getName(), typeParams -> SetType.set(typeParams.get(0)));
  }

  public static Type getJavaType(Type rawType, Type... typeParameters) {
    if (typeParameters.length == 0) {
      return rawType;
    } else {
      return new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
          return typeParameters;
        }

        @Override
        public Type getRawType() {
          return rawType;
        }

        @Override
        @Nullable
        public Type getOwnerType() {
          return null;
        }
      };
    }
  }

  public static DataType<?> getDataType(
      String canonicalClassName, List<? extends DataType<?>> typeParameters) {
    if (mappings.containsKey(canonicalClassName)) {
      return mappings.get(canonicalClassName).apply(typeParameters);
    } else {
      return CustomType.create(canonicalClassName, typeParameters);
    }
  }

  private TypeUtils() {}
}
