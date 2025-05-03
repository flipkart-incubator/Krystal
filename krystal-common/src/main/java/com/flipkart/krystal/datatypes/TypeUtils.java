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
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;

@UtilityClass
public final class TypeUtils {

  static final Map<String, Function<DataType<?>[], JavaType<?>>> dataTypeMappings =
      new LinkedHashMap<>();

  static final Set<Class<?>> NON_PRIMITIVE_CLASSES_WITH_PLATFORM_DEFAULTS =
      Set.of(String.class, List.class, Map.class);

  static {
    dataTypeMappings.put(boolean.class.getCanonicalName(), _unused -> BOOLEAN);
    dataTypeMappings.put(Boolean.class.getCanonicalName(), _unused -> BOOLEAN);

    dataTypeMappings.put(int.class.getCanonicalName(), _unused -> INT);
    dataTypeMappings.put(Integer.class.getCanonicalName(), _unused -> INT);

    dataTypeMappings.put(byte.class.getCanonicalName(), _unused -> BYTE);
    dataTypeMappings.put(Byte.class.getCanonicalName(), _unused -> BYTE);

    dataTypeMappings.put(short.class.getCanonicalName(), _unused -> SHORT);
    dataTypeMappings.put(Short.class.getCanonicalName(), _unused -> SHORT);

    dataTypeMappings.put(long.class.getCanonicalName(), _unused -> LONG);
    dataTypeMappings.put(Long.class.getCanonicalName(), _unused -> LONG);

    dataTypeMappings.put(char.class.getCanonicalName(), _unused -> CHAR);
    dataTypeMappings.put(Character.class.getCanonicalName(), _unused -> CHAR);

    dataTypeMappings.put(char.class.getCanonicalName(), _unused -> FLOAT);
    dataTypeMappings.put(Character.class.getCanonicalName(), _unused -> FLOAT);

    dataTypeMappings.put(char.class.getCanonicalName(), _unused -> DOUBLE);
    dataTypeMappings.put(Character.class.getCanonicalName(), _unused -> DOUBLE);

    dataTypeMappings.put(String.class.getCanonicalName(), _unused -> STRING);
    dataTypeMappings.put(Object.class.getCanonicalName(), _unused -> OBJECT);

    dataTypeMappings.put(void.class.getCanonicalName(), _unused -> VOID);
    dataTypeMappings.put(Void.class.getCanonicalName(), _unused -> VOID);

    dataTypeMappings.put(
        List.class.getName(),
        typeParams -> typeParams.length == 0 ? LIST_RAW : new JavaType<>(List.class, typeParams));
    dataTypeMappings.put(
        Set.class.getName(),
        typeParams -> typeParams.length == 0 ? MAP_RAW : new JavaType<>(Set.class, typeParams));
  }

  static Type getJavaType(Class<?> rawType, Type... typeParameters) {
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

  static boolean hasPlatformDefaultValue(TypeMirror t) {
    char[][][] c = new char[][][] {};
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
}
