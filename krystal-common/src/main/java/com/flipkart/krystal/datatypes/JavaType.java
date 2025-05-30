package com.flipkart.krystal.datatypes;

import static com.flipkart.krystal.datatypes.TypeUtils.dataTypeMappings;
import static com.flipkart.krystal.datatypes.TypeUtils.getJavaType;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@EqualsAndHashCode(of = {"canonicalClassName", "typeParameters"})
public final class JavaType<T> implements DataType<T> {

  /** the fully qualified name of the class, i.e. pck.outer.inner */
  @Getter private final @lombok.NonNull String canonicalClassName;

  @Getter private final ImmutableList<DataType<?>> typeParameters;

  /** Number of array dimensions if this type is an array, 0 otherwise */
  @Getter private final int numberOfArrayDimensions;

  private @MonotonicNonNull Type type;

  @SuppressWarnings("unchecked")
  public static <T> JavaType<@NonNull T> create(Class<?> clazz, DataType<?>... typeParams) {
    String canonicalClassName = clazz.getCanonicalName();
    if (canonicalClassName != null && dataTypeMappings.containsKey(canonicalClassName)) {
      return (JavaType<@NonNull T>) dataTypeMappings.get(canonicalClassName).apply(typeParams);
    } else {
      return new JavaType<>(clazz, typeParams);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> JavaType<@NonNull T> create(
      String canonicalClassName, DataType<?>... typeParameters) {
    if (dataTypeMappings.containsKey(canonicalClassName)) {
      // noinspection unchecked
      return (JavaType<@NonNull T>) dataTypeMappings.get(canonicalClassName).apply(typeParameters);
    } else {
      return new JavaType<>(canonicalClassName, typeParameters);
    }
  }

  JavaType(Class<?> clazz, DataType<?>... typeParameters) {
    this(requireNonNull(clazz.getCanonicalName()), clazz, typeParameters);
  }

  private JavaType(String canonicalClassName, DataType<?>... typeParameters) {
    this(canonicalClassName, null, typeParameters);
  }

  private JavaType(
      String canonicalClassName, @Nullable Class<?> clazz, DataType<?>... typeParameters) {
    this.type = clazz;
    this.typeParameters = ImmutableList.copyOf(typeParameters);
    this.canonicalClassName = canonicalClassName;
    if (canonicalClassName.startsWith("@")) {
      throw new UnsupportedOperationException("Annotations not supported : " + canonicalClassName);
    }

    // Detect if the canonical class name represents an array and count dimensions
    int dimensions = 0;
    if (canonicalClassName.endsWith("[]")) {
      String temp = canonicalClassName;
      while (temp.endsWith("[]")) {
        dimensions++;
        temp = temp.substring(0, temp.length() - 2);
      }
    }
    this.numberOfArrayDimensions = dimensions;
  }

  @Override
  public Type javaReflectType() throws ClassNotFoundException {
    if (type == null) {
      @SuppressWarnings("unchecked")
      Class<T> type =
          (Class<T>) checkNotNull(this.getClass().getClassLoader()).loadClass(canonicalClassName());

      List<Type> list = new ArrayList<>();
      for (DataType<?> typeParameter : typeParameters) {
        Type javaReflectType = typeParameter.javaReflectType();
        list.add(javaReflectType);
      }
      // noinspection ZeroLengthArrayAllocation
      this.type = getJavaType(type, list.toArray(new Type[0]));
    }
    return type;
  }

  @Override
  public String toString() {
    return canonicalClassName()
        + (typeParameters.isEmpty()
            ? ""
            : "< "
                + typeParameters.stream().map(Objects::toString).collect(Collectors.joining(", "))
                + ">");
  }
}
