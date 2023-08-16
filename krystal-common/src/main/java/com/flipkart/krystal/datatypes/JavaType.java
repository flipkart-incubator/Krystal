package com.flipkart.krystal.datatypes;

import static java.util.function.Function.identity;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Accessors(fluent = true)
public final class JavaType<T> implements JavaDataType<T> {

  private final String className;
  @MonotonicNonNull private String packageName;
  @MonotonicNonNull private String simpleName;
  private ImmutableList<String> enclosingClasses = ImmutableList.of();
  @Getter private ImmutableList<DataType> typeParameters = ImmutableList.of();

  @MonotonicNonNull private Type clazz;

  private JavaType(Class<T> clazz) {
    this(clazz.getPackageName(), clazz.getSimpleName(), getEnclosingClasses(clazz));
    this.clazz = clazz;
  }

  public JavaType(String className) {
    this.className = className;
  }

  private JavaType(String className, List<? extends DataType> typeParameters) {
    this.className = className;
    this.typeParameters = ImmutableList.copyOf(typeParameters);
  }

  public JavaType(String packageName, String simpleName, ImmutableList<String> enclosingClasses) {
    this(packageName, simpleName, enclosingClasses, new ArrayList<>());
  }

  public JavaType(
      String packageName,
      String simpleName,
      List<String> enclosingClasses,
      List<? extends DataType> typeParameters) {
    this(
        Stream.of(Stream.of(packageName), enclosingClasses.stream(), Stream.of(simpleName))
            .flatMap(identity())
            .collect(Collectors.joining(".")));
    this.packageName = packageName;
    this.simpleName = simpleName;
    this.enclosingClasses = ImmutableList.copyOf(enclosingClasses);
    this.typeParameters = ImmutableList.copyOf(typeParameters);
  }

  private static ImmutableList<String> getEnclosingClasses(Class<?> clazz) {
    Deque<String> enclosingClasses = new ArrayDeque<>();
    Class<?> enclosingClass = clazz;
    while ((enclosingClass = enclosingClass.getEnclosingClass()) != null) {
      enclosingClasses.push(enclosingClass.getSimpleName());
    }
    return ImmutableList.copyOf(enclosingClasses);
  }

  public String className() {
    return className;
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
  public Optional<Type> javaType() {
    if (clazz == null) {
      try {
        //noinspection unchecked
        Class<T> type =
            (Class<T>)
                Optional.ofNullable(this.getClass().getClassLoader())
                    .orElseThrow(
                        () ->
                            new IllegalStateException(
                                "null classloader returned. Cannot proceed further"))
                    .loadClass(className());
        this.clazz = type;
        if (!typeParameters.isEmpty()) {
          if (typeParameters.stream().map(TypeUtils::getJavaType).allMatch(Optional::isPresent)) {
            this.clazz =
                new ParameterizedType() {
                  @Override
                  public Type[] getActualTypeArguments() {
                    return typeParameters.stream()
                        .map(TypeUtils::getJavaType)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toArray(Type[]::new);
                  }

                  @Override
                  public Type getRawType() {
                    return type;
                  }

                  @Override
                  @Nullable
                  public Type getOwnerType() {
                    return null;
                  }
                };
          } else {
            return Optional.empty();
          }
        }
      } catch (ClassNotFoundException e) {
        return Optional.empty();
      }
    }
    return Optional.of(clazz);
  }

  public static <T> JavaType<T> java(Class<T> clazz) {
    return new JavaType<>(clazz);
  }

  public static <T> JavaType<T> java(
      String packageName,
      String simpleName,
      List<String> enclosingClasses,
      List<? extends DataType> typeParameters) {
    return new JavaType<>(packageName, simpleName, enclosingClasses, typeParameters);
  }

  public static <T> JavaType<T> java(
      List<String> typeInfo, List<? extends DataType> typeParameters) {
    if (typeInfo.size() < 1) {
      throw new IllegalArgumentException("At least one type name is needed for java types.");
    }
    if (typeInfo.size() == 1) {
      return JavaType.java(typeInfo.get(0), typeParameters);
    }
    return JavaType.java(
        typeInfo.get(0),
        typeInfo.get(typeInfo.size() - 1),
        typeInfo.subList(1, typeInfo.size() - 1),
        typeParameters);
  }

  public static <T> JavaType<T> java(String className) {
    return new JavaType<T>(className);
  }

  public static <T> JavaType<T> java(String className, List<? extends DataType> typeParameters) {
    return new JavaType<T>(className, typeParameters);
  }
}
