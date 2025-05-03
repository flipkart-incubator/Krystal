package com.flipkart.krystal.datatypes;

import static com.flipkart.krystal.datatypes.TypeUtils.box;
import static com.flipkart.krystal.datatypes.TypeUtils.dataTypeMappings;
import static com.flipkart.krystal.datatypes.TypeUtils.getJavaType;
import static com.flipkart.krystal.datatypes.TypeUtils.typeKindMappings;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.function.Function.identity;

import com.google.common.base.Defaults;
import com.google.common.collect.ImmutableList;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
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

  private @MonotonicNonNull Type clazz;

  private @MonotonicNonNull T platformDefaultValue;

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
    this(
        Stream.of(
                Optional.ofNullable(clazz.getPackage()).map(Package::getName).stream(),
                getEnclosingClasses(clazz).stream(),
                Stream.of(clazz.getSimpleName()))
            .flatMap(identity())
            .filter(Objects::nonNull)
            .collect(Collectors.joining(".")),
        clazz,
        typeParameters);
  }

  private JavaType(String canonicalClassName, DataType<?>... typeParameters) {
    this(canonicalClassName, null, typeParameters);
  }

  private JavaType(
      @NonNull String canonicalClassName, @Nullable Class<?> clazz, DataType<?>... typeParameters) {
    this.clazz = clazz;
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
    if (clazz == null) {
      @SuppressWarnings("unchecked")
      Class<T> type =
          (Class<T>) checkNotNull(this.getClass().getClassLoader()).loadClass(canonicalClassName());

      List<Type> list = new ArrayList<>();
      for (DataType<?> typeParameter : typeParameters) {
        Type javaReflectType = typeParameter.javaReflectType();
        list.add(javaReflectType);
      }
      // noinspection ZeroLengthArrayAllocation
      this.clazz = getJavaType(type, list.toArray(new Type[0]));
    }
    return clazz;
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    TypeKind typeKind = typeKindMappings.get(canonicalClassName);
    if (typeKind != null) {
      if (typeKind.isPrimitive()) {
        return processingEnv.getTypeUtils().getPrimitiveType(typeKind);
      }
      if (typeKind == TypeKind.VOID) {
        return processingEnv.getTypeUtils().getNoType(typeKind);
      }
    }
    TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(canonicalClassName);
    if (typeElement == null) {
      throw new IllegalArgumentException(
          "Could not find typeElement for canonical class name %s".formatted(canonicalClassName));
    }
    return processingEnv
        .getTypeUtils()
        .getDeclaredType(
            typeElement,
            typeParameters.stream()
                .map(t -> box(t.javaModelType(processingEnv), processingEnv))
                .toArray(TypeMirror[]::new));
  }

  @Override
  public @NonNull T getPlatformDefaultValue() {
    if (platformDefaultValue == null) {
      platformDefaultValue = computeDefaultValue();
    }
    return platformDefaultValue;
  }

  @Override
  public boolean hasPlatformDefaultValue(ProcessingEnvironment processingEnv) {
    return TypeUtils.hasPlatformDefaultValue(javaModelType(processingEnv));
  }

  @Override
  public DataType<T> rawType() {
    return JavaType.create(canonicalClassName());
  }

  @SuppressWarnings("unchecked")
  private @NonNull T computeDefaultValue() {
    try {
      Type type = javaReflectType();
      if (type instanceof Class<?> c) {
        T defaultPrimitiveValue = (T) Defaults.defaultValue(c);
        if (defaultPrimitiveValue != null) {
          return defaultPrimitiveValue;
        } else {
          T defaultNonPrimitiveValue = defaultNonPrimitiveValue(c);
          if (defaultNonPrimitiveValue != null) {
            return defaultNonPrimitiveValue;
          }
        }
      } else if (type instanceof ArrayType) {
        return (@NonNull T) new Object[0];
      } else if (type instanceof ParameterizedType p) {
        Type rawType = p.getRawType();
        if (rawType instanceof Class<?> c) {
          T defaultValue = defaultNonPrimitiveValue(c);
          if (defaultValue != null) {
            return defaultValue;
          }
        }
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    throw new IllegalArgumentException(
        "Cannot determine platform default value for type %s".formatted(this));
  }

  @SuppressWarnings("unchecked")
  private @Nullable T defaultNonPrimitiveValue(Class<?> c) {
    if (String.class.isAssignableFrom(c)) {
      return (T) "";
    } else if (List.class.isAssignableFrom(c)) {
      return (T) List.of();
    } else if (Map.class.isAssignableFrom(c)) {
      return (T) Map.of();
    }
    return null;
  }

  private static ImmutableList<String> getEnclosingClasses(Class<?> clazz) {
    Deque<String> enclosingClasses = new ArrayDeque<>();
    Class<?> enclosingClass = clazz;
    while ((enclosingClass = enclosingClass.getEnclosingClass()) != null) {
      enclosingClasses.push(enclosingClass.getSimpleName());
    }
    return ImmutableList.copyOf(enclosingClasses);
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
