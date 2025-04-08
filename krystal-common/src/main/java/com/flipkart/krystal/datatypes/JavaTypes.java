package com.flipkart.krystal.datatypes;

import java.util.List;
import java.util.Map;

/**
 * {@link JavaType}s corresponding to java primitives represent both the primitive type and boxed
 * type of the primitive
 */
public final class JavaTypes {

  /** JavaType for boolean and {@link Boolean} */
  public static final JavaType<Boolean> BOOLEAN = new JavaType<>(boolean.class);

  /** JavaType for byte and {@link Byte} */
  public static final JavaType<Byte> BYTE = new JavaType<>(byte.class);

  /** JavaType for char and {@link Character} */
  public static final JavaType<Character> CHAR = new JavaType<>(char.class);

  /** JavaType for short and {@link Short} */
  public static final JavaType<Short> SHORT = new JavaType<>(short.class);

  /** JavaType for int and {@link Integer} */
  public static final JavaType<Integer> INT = new JavaType<>(int.class);

  /** JavaType for long and {@link Long} */
  public static final JavaType<Long> LONG = new JavaType<>(long.class);

  /** JavaType for float and {@link Float} */
  public static final JavaType<Float> FLOAT = new JavaType<>(float.class);

  /** JavaType for double and {@link Double} */
  public static final JavaType<Double> DOUBLE = new JavaType<>(double.class);

  /** JavaType for {@link String} */
  public static final JavaType<String> STRING = new JavaType<>(String.class);

  /** JavaType for the raw {@link List} (without any type parameters) */
  public static final JavaType<List> LIST_RAW = new JavaType<>(List.class);

  /** JavaType for the raw {@link Map} (without any type parameters)  */
  public static final JavaType<Map> MAP_RAW = new JavaType<>(Map.class);

  private JavaTypes() {}
}
