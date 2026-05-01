package com.flipkart.krystal.vajram.protobuf.util;

import com.flipkart.krystal.model.EnumModel;
import com.google.protobuf.ProtocolMessageEnum;

/**
 * Utility class for converting between proto enum values and Java {@link EnumModel} enum values.
 * The conversion is name-based: proto enum constant names match Java enum constant names. If a
 * proto enum value has no matching Java constant (e.g., UNRECOGNIZED), it falls back to the first
 * Java constant (which must be UNKNOWN by convention).
 */
public final class ProtoEnumUtils {

  /**
   * Converts a proto enum value to the corresponding Java enum value. Falls back to the first
   * constant (UNKNOWN) if the proto value has no match.
   *
   * @param <E> the Java enum type implementing EnumModel
   * @param enumClass the Java enum class
   * @param protoEnumName the name of the proto enum constant
   * @return the matching Java enum constant, or the first constant (UNKNOWN) if no match
   */
  public static <E extends Enum<E> & EnumModel> E protoToJava(
      Class<E> enumClass, String protoEnumName) {
    try {
      return Enum.valueOf(enumClass, protoEnumName);
    } catch (IllegalArgumentException e) {
      // Fall back to first constant (ex: UNKNOWN)
      return enumClass.getEnumConstants()[0];
    }
  }

  /**
   * Converts a Java enum value to the corresponding proto enum value by name.
   *
   * @param <P> the proto enum type
   * @param protoEnumClass the proto enum class
   * @param javaEnumName the name of the Java enum constant
   * @return the matching proto enum constant
   */
  public static <P extends Enum<P> & ProtocolMessageEnum> P javaToProto(
      Class<P> protoEnumClass, String javaEnumName) {
    return Enum.valueOf(protoEnumClass, javaEnumName);
  }

  private ProtoEnumUtils() {}
}
