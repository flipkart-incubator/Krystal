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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@EqualsAndHashCode(of = {"canonicalClassName", "typeParameters"})
public final class JavaType<T> implements DataType<T> {

  /** the fully qualified name of the class, i.e. pck.outer.inner */
  private final String canonicalClassName;

  private @MonotonicNonNull String packageName;
  private @MonotonicNonNull String simpleName;
  private ImmutableList<String> enclosingClasses = ImmutableList.of();
  @Getter private final ImmutableList<DataType<?>> typeParameters;

  private @MonotonicNonNull Type clazz;

  JavaType(Class<?> clazz, List<? extends DataType<?>> typeParams) {
    this(
        Optional.ofNullable(clazz.getPackage()).map(Package::getName),
        clazz.getSimpleName(),
        getEnclosingClasses(clazz),
        typeParams);
    this.clazz = clazz;
  }

  @SuppressWarnings({
    "UnnecessaryTypeArgument",
    "optional.parameter"
  }) // -> To prevent Null checker errors
  private JavaType(
      Optional<String> packageName,
      String simpleName,
      List<String> enclosingClasses,
      List<? extends DataType<?>> typeParameters) {
    this(
        Stream.of(packageName.stream(), enclosingClasses.stream(), Stream.of(simpleName))
            .flatMap(identity())
            .filter(Objects::nonNull)
            .collect(Collectors.joining(".")),
        typeParameters);
    this.packageName = packageName.orElse(null);
    this.simpleName = simpleName;
    this.enclosingClasses = ImmutableList.copyOf(enclosingClasses);
  }

  private JavaType(String canonicalClassName, List<? extends DataType<?>> typeParameters) {
    this.canonicalClassName = canonicalClassName;
    this.typeParameters = ImmutableList.copyOf(typeParameters);
  }

  public static <T> JavaType<T> create(Class<T> clazz) {
    return create(clazz, ImmutableList.of());
  }

  @SuppressWarnings("unchecked")
  public static <T> JavaType<T> create(Class<?> clazz, List<DataType<?>> typeParams) {
    String canonicalClassName = clazz.getCanonicalName();
    if (canonicalClassName != null && dataTypeMappings.containsKey(canonicalClassName)) {
      return (JavaType<T>) dataTypeMappings.get(canonicalClassName).apply(typeParams);
    } else {
      return new JavaType<T>(clazz, typeParams);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> JavaType<T> create(
      String canonicalClassName, List<? extends DataType<?>> typeParameters) {
    if (dataTypeMappings.containsKey(canonicalClassName)) {
      //noinspection unchecked
      return (JavaType<T>) dataTypeMappings.get(canonicalClassName).apply(typeParameters);
    } else {
      return new JavaType<>(canonicalClassName, typeParameters);
    }
  }

  public String canonicalClassName() {
    return canonicalClassName;
  }

  public Optional<String> packageName() {
    return Optional.ofNullable(packageName);
  }

  public Optional<String> simpleName() {
    return Optional.ofNullable(simpleName);
  }

  public ImmutableList<String> enclosingClasses() {
    return enclosingClasses;
  }

  @Override
  public Type javaReflectType() throws ClassNotFoundException {
    if (clazz == null) {
      if (!enclosingClasses.isEmpty()) {
        throw new UnsupportedOperationException(
            "Cannot load java type of an enclosed class - only top level classes supported");
      }
      @SuppressWarnings("unchecked")
      Class<T> type =
          (Class<T>) checkNotNull(this.getClass().getClassLoader()).loadClass(canonicalClassName());

      List<Type> list = new ArrayList<>();
      for (DataType<?> typeParameter : typeParameters) {
        Type javaReflectType = typeParameter.javaReflectType();
        list.add(javaReflectType);
      }
      //noinspection ZeroLengthArrayAllocation
      this.clazz = getJavaType(type, list.toArray(new Type[0]));
    }
    return clazz;
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    if (this.clazz != null) {
      TypeKind typeKind = typeKindMappings.get(this.clazz);
      if (typeKind != null && typeKind.isPrimitive()) {
        return processingEnv.getTypeUtils().getPrimitiveType(typeKind);
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
  @SuppressWarnings("unchecked")
  public @NonNull T getPlatformDefaultValue() {
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

  @Override
  public boolean hasPlatformDefaultValue(ProcessingEnvironment processingEnv) {
    return TypeUtils.hasPlatformDefaultValue(javaModelType(processingEnv));
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
    try {
      return javaReflectType().toString();
    } catch (ClassNotFoundException e) {
      return super.toString();
    }
  }
}
