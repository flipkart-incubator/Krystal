package com.flipkart.krystal.datatypes;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TypeUtils {

  static final Map<String, Function<List<? extends DataType<?>>, JavaType<?>>> dataTypeMappings =
      new LinkedHashMap<>();

  static final Map<Type, TypeKind> typeKindMappings = new LinkedHashMap<>();

  static final Set<Class<?>> NON_PRIMITIVE_CLASSES_WITH_PLATFORM_DEFAULTS =
      Set.of(String.class, List.class, Map.class);

  static {
    dataTypeMappings.put(
        boolean.class.getName(), _unused -> new JavaType<>(boolean.class, ImmutableList.of()));
    dataTypeMappings.put(
        Boolean.class.getName(), _unused -> new JavaType<>(boolean.class, ImmutableList.of()));
    typeKindMappings.put(boolean.class, TypeKind.BOOLEAN);
    typeKindMappings.put(Boolean.class, TypeKind.BOOLEAN);

    dataTypeMappings.put(
        int.class.getName(), _unused -> new JavaType<>(int.class, ImmutableList.of()));
    dataTypeMappings.put(
        Integer.class.getName(), _unused -> new JavaType<>(int.class, ImmutableList.of()));
    typeKindMappings.put(int.class, TypeKind.INT);
    typeKindMappings.put(Integer.class, TypeKind.INT);

    dataTypeMappings.put(
        String.class.getName(), _unused -> new JavaType<>(String.class, ImmutableList.of()));
    dataTypeMappings.put(
        List.class.getName(),
        typeParams -> new JavaType<>(List.class, ImmutableList.of(typeParams.get(0))));
    dataTypeMappings.put(
        Set.class.getName(),
        typeParams -> new JavaType<>(Set.class, ImmutableList.of(typeParams.get(0))));

    typeKindMappings.put(byte.class, TypeKind.BYTE);
    typeKindMappings.put(Byte.class, TypeKind.BYTE);

    typeKindMappings.put(short.class, TypeKind.SHORT);
    typeKindMappings.put(Short.class, TypeKind.SHORT);

    typeKindMappings.put(long.class, TypeKind.LONG);
    typeKindMappings.put(Long.class, TypeKind.LONG);

    typeKindMappings.put(char.class, TypeKind.CHAR);
    typeKindMappings.put(Character.class, TypeKind.CHAR);

    typeKindMappings.put(float.class, TypeKind.FLOAT);
    typeKindMappings.put(Float.class, TypeKind.FLOAT);

    typeKindMappings.put(double.class, TypeKind.DOUBLE);
    typeKindMappings.put(Double.class, TypeKind.DOUBLE);
  }

  static Type getJavaType(Type rawType, Type... typeParameters) {
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

  static TypeMirror box(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    if (typeMirror.getKind().isPrimitive()) {
      return processingEnv.getTypeUtils().boxedClass((PrimitiveType) typeMirror).asType();
    } else {
      return typeMirror;
    }
  }

  static boolean hasPlatformDefaultValue(TypeMirror t) {
    if (t instanceof PrimitiveType) {
      return true;
    } else if (t instanceof ArrayType) {
      return true;
    } else if (t instanceof DeclaredType declaredType
        && declaredType.asElement() instanceof TypeElement typeElement) {
      return NON_PRIMITIVE_CLASSES_WITH_PLATFORM_DEFAULTS.stream()
          .anyMatch(
              aClass ->
                  typeElement
                      .getQualifiedName()
                      .contentEquals(checkNotNull(aClass.getCanonicalName())));
    }
    return false;
  }

  private TypeUtils() {}
}
