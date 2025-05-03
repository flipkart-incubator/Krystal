package com.flipkart.krystal.datatypes;

import static com.flipkart.krystal.datatypes.JavaTypes.BOOLEAN;
import static com.flipkart.krystal.datatypes.JavaTypes.BYTE;
import static com.flipkart.krystal.datatypes.JavaTypes.CHAR;
import static com.flipkart.krystal.datatypes.JavaTypes.DOUBLE;
import static com.flipkart.krystal.datatypes.JavaTypes.FLOAT;
import static com.flipkart.krystal.datatypes.JavaTypes.INT;
import static com.flipkart.krystal.datatypes.JavaTypes.LIST_RAW;
import static com.flipkart.krystal.datatypes.JavaTypes.LONG;
import static com.flipkart.krystal.datatypes.JavaTypes.MAP_RAW;
import static com.flipkart.krystal.datatypes.JavaTypes.OBJECT;
import static com.flipkart.krystal.datatypes.JavaTypes.SHORT;
import static com.flipkart.krystal.datatypes.JavaTypes.STRING;
import static com.flipkart.krystal.datatypes.JavaTypes.VOID;
import static com.google.common.base.Preconditions.checkNotNull;

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

  static final Map<String, Function<DataType<?>[], JavaType<?>>> dataTypeMappings =
      new LinkedHashMap<>();

  static final Map<String, TypeKind> typeKindMappings = new LinkedHashMap<>();

  static final Set<Class<?>> NON_PRIMITIVE_CLASSES_WITH_PLATFORM_DEFAULTS =
      Set.of(String.class, List.class, Map.class);

  static {
    dataTypeMappings.put(boolean.class.getCanonicalName(), _unused -> BOOLEAN);
    dataTypeMappings.put(Boolean.class.getCanonicalName(), _unused -> BOOLEAN);
    typeKindMappings.put(boolean.class.getCanonicalName(), TypeKind.BOOLEAN);
    typeKindMappings.put(Boolean.class.getCanonicalName(), TypeKind.BOOLEAN);

    dataTypeMappings.put(int.class.getCanonicalName(), _unused -> INT);
    dataTypeMappings.put(Integer.class.getCanonicalName(), _unused -> INT);
    typeKindMappings.put(int.class.getCanonicalName(), TypeKind.INT);
    typeKindMappings.put(Integer.class.getCanonicalName(), TypeKind.INT);

    dataTypeMappings.put(byte.class.getCanonicalName(), _unused -> BYTE);
    dataTypeMappings.put(Byte.class.getCanonicalName(), _unused -> BYTE);
    typeKindMappings.put(byte.class.getCanonicalName(), TypeKind.BYTE);
    typeKindMappings.put(Byte.class.getCanonicalName(), TypeKind.BYTE);

    dataTypeMappings.put(short.class.getCanonicalName(), _unused -> SHORT);
    dataTypeMappings.put(Short.class.getCanonicalName(), _unused -> SHORT);
    typeKindMappings.put(short.class.getCanonicalName(), TypeKind.SHORT);
    typeKindMappings.put(Short.class.getCanonicalName(), TypeKind.SHORT);

    dataTypeMappings.put(long.class.getCanonicalName(), _unused -> LONG);
    dataTypeMappings.put(Long.class.getCanonicalName(), _unused -> LONG);
    typeKindMappings.put(long.class.getCanonicalName(), TypeKind.LONG);
    typeKindMappings.put(Long.class.getCanonicalName(), TypeKind.LONG);

    dataTypeMappings.put(char.class.getCanonicalName(), _unused -> CHAR);
    dataTypeMappings.put(Character.class.getCanonicalName(), _unused -> CHAR);
    typeKindMappings.put(char.class.getCanonicalName(), TypeKind.CHAR);
    typeKindMappings.put(Character.class.getCanonicalName(), TypeKind.CHAR);

    dataTypeMappings.put(char.class.getCanonicalName(), _unused -> FLOAT);
    dataTypeMappings.put(Character.class.getCanonicalName(), _unused -> FLOAT);
    typeKindMappings.put(float.class.getCanonicalName(), TypeKind.FLOAT);
    typeKindMappings.put(Float.class.getCanonicalName(), TypeKind.FLOAT);

    dataTypeMappings.put(char.class.getCanonicalName(), _unused -> DOUBLE);
    dataTypeMappings.put(Character.class.getCanonicalName(), _unused -> DOUBLE);
    typeKindMappings.put(double.class.getCanonicalName(), TypeKind.DOUBLE);
    typeKindMappings.put(Double.class.getCanonicalName(), TypeKind.DOUBLE);

    dataTypeMappings.put(String.class.getCanonicalName(), _unused -> STRING);
    dataTypeMappings.put(Object.class.getCanonicalName(), _unused -> OBJECT);

    dataTypeMappings.put(void.class.getCanonicalName(), _unused -> VOID);
    dataTypeMappings.put(Void.class.getCanonicalName(), _unused -> VOID);
    typeKindMappings.put(void.class.getCanonicalName(), TypeKind.VOID);
    typeKindMappings.put(Void.class.getCanonicalName(), TypeKind.VOID);

    dataTypeMappings.put(
        List.class.getName(),
        typeParams -> typeParams.length == 0 ? LIST_RAW : new JavaType<>(List.class, typeParams));
    dataTypeMappings.put(
        Set.class.getName(),
        typeParams -> typeParams.length == 0 ? MAP_RAW : new JavaType<>(Set.class, typeParams));
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
    if (t.getKind().isPrimitive()) {
      return true;
    } else if (TypeKind.ARRAY.equals(t.getKind())) {
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
