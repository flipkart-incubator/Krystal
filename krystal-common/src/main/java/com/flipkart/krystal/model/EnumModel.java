package com.flipkart.krystal.model;

/**
 * Marker interface for Krystal model enums. Java enums annotated with {@link ModelRoot} should
 * implement this interface.
 *
 * <p>Enum models must satisfy the following constraints:
 *
 * <ul>
 *   <li>The first enum constant must always be {@code UNKNOWN}.
 *   <li>If {@link com.flipkart.krystal.serial.SerialId @SerialId} is used on any constant, {@code
 *       UNKNOWN} must have {@code @SerialId(0)}.
 * </ul>
 *
 * <p>In JSON serialization, if the deserializer encounters a value not present in the enum, it will
 * be deserialized to {@code UNKNOWN}.
 *
 * <p>In Protobuf serialization, a proto enum is generated. The proto index for enum values matches
 * the declaration order (ordinal) unless overridden by {@code @SerialId}.
 */
public interface EnumModel {}
