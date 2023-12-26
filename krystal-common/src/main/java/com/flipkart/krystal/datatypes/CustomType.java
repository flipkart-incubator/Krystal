package com.flipkart.krystal.datatypes;

import static com.flipkart.krystal.datatypes.TypeUtils.box;
import static com.flipkart.krystal.datatypes.TypeUtils.getJavaType;
import static java.util.function.Function.identity;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

@Accessors(fluent = true)
@ToString
@EqualsAndHashCode(
    of = {"canonicalClassName", "typeParameters"},
    callSuper = false)
public final class CustomType<T> extends AbstractDataType<T> {

  /** the fully qualified name of the class, i.e. pck.outer.inner */
  private final String canonicalClassName;

  private @MonotonicNonNull String packageName;
  private @MonotonicNonNull String simpleName;
  private ImmutableList<String> enclosingClasses = ImmutableList.of();
  @Getter private ImmutableList<DataType<?>> typeParameters = ImmutableList.of();

  private @MonotonicNonNull Type clazz;

  private CustomType(Class<T> clazz) {
    this(clazz.getPackageName(), clazz.getSimpleName(), getEnclosingClasses(clazz));
    this.clazz = clazz;
  }

  public CustomType(String canonicalClassName) {
    this.canonicalClassName = canonicalClassName;
  }

  private CustomType(String canonicalClassName, List<? extends DataType<?>> typeParameters) {
    this.canonicalClassName = canonicalClassName;
    this.typeParameters = ImmutableList.copyOf(typeParameters);
  }

  private CustomType(
      String packageName, String simpleName, ImmutableList<String> enclosingClasses) {
    this(packageName, simpleName, enclosingClasses, new ArrayList<>());
  }

  private CustomType(
      String packageName,
      String simpleName,
      List<String> enclosingClasses,
      List<? extends DataType<?>> typeParameters) {
    this(
        Stream.of(Stream.of(packageName), enclosingClasses.stream(), Stream.of(simpleName))
            .flatMap(identity())
            .collect(Collectors.joining(".")));
    this.packageName = packageName;
    this.simpleName = simpleName;
    this.enclosingClasses = ImmutableList.copyOf(enclosingClasses);
    this.typeParameters = ImmutableList.copyOf(typeParameters);
  }

  public static <T> CustomType<T> create(Class<T> clazz) {
    return new CustomType<>(clazz);
  }

  public static <T> CustomType<T> create(
      String packageName,
      String simpleName,
      List<String> enclosingClasses,
      List<? extends DataType<?>> typeParameters) {
    return new CustomType<>(packageName, simpleName, enclosingClasses, typeParameters);
  }

  public static <T> CustomType<T> create(
      List<String> typeInfo, List<? extends DataType<?>> typeParameters) {
    if (typeInfo.isEmpty()) {
      throw new IllegalArgumentException("At least one type name is needed for java types.");
    }
    if (typeInfo.size() == 1) {
      return create(typeInfo.get(0), typeParameters);
    }
    return create(
        typeInfo.get(0),
        typeInfo.get(typeInfo.size() - 1),
        typeInfo.subList(1, typeInfo.size() - 1),
        typeParameters);
  }

  public static <T> CustomType<T> create(
      String className, List<? extends DataType<?>> typeParameters) {
    return new CustomType<>(className, typeParameters);
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
      //noinspection unchecked
      Class<T> type =
          (Class<T>)
              Optional.ofNullable(this.getClass().getClassLoader())
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "null classloader returned. Cannot proceed further"))
                  .loadClass(canonicalClassName());

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
    TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(canonicalClassName);
    return processingEnv
        .getTypeUtils()
        .getDeclaredType(
            typeElement,
            typeParameters.stream()
                .map(t -> box(t.javaModelType(processingEnv), processingEnv))
                .toArray(TypeMirror[]::new));
  }

  private static ImmutableList<String> getEnclosingClasses(Class<?> clazz) {
    Deque<String> enclosingClasses = new ArrayDeque<>();
    Class<?> enclosingClass = clazz;
    while ((enclosingClass = enclosingClass.getEnclosingClass()) != null) {
      enclosingClasses.push(enclosingClass.getSimpleName());
    }
    return ImmutableList.copyOf(enclosingClasses);
  }
}
