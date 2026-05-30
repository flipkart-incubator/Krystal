package com.flipkart.krystal.vajram.protobuf.util;

import com.flipkart.krystal.annos.NoAnnotation;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.array.ByteArray;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.serial.SerializableModel;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Marker interface for SerdeProtocols that represent a protobuf flavour (proto3, edition 2024,
 * etc.). Allows protocol-agnostic codegen modules (e.g. lattice-grpc-codegen) to identify and work
 * with any supported protobuf protocol without depending on each protobuf module directly.
 *
 * <p>Each implementation also exposes the protocol-specific schema strings (header line, generated
 * file suffix) so downstream codegen can compose schemas without taking a hard dependency on a
 * particular protobuf module.
 */
public interface ProtobufProtocol extends SerdeProtocol<NoAnnotation, SerializableModel> {

  /**
   * Header line that opens a {@code .proto} file for this protocol. Examples: {@code syntax =
   * "proto3";}, {@code edition = "2024";}.
   */
  String schemaHeader();

  /**
   * Suffix (including the {@code .proto} extension) appended to the model name to derive the
   * generated proto schema file name. Examples: {@code .models.proto3.proto} for proto3, {@code
   * .models.proto} for edition 2024.
   */
  String protoFileSuffix();

  /**
   * Suffix appended to the (de-underscored) model name to derive the protoc-generated message class
   * name. Equals {@link #modelClassesSuffix()} - e.g. {@code Proto3} for proto3, {@code Proto} for
   * edition 2024. The caller must strip underscores from the model name first (proto message names
   * must be TitleCase under editions 2024+).
   */
  default String protoMsgSuffix() {
    return modelClassesSuffix();
  }

  /**
   * Whether to emit {@code option java_multiple_files = true;} in generated {@code .proto} files.
   * Defaults to true (proto3 default is single-file mode). Editions 2024+ default to multi-file
   * already and reject the explicit option, so they should override this to false.
   */
  default boolean emitJavaMultipleFiles() {
    return true;
  }

  @Override
  default ByteArray serialize(
      Object object,
      Function<Model, SerializableModel> modelMapper,
      @Nullable NoAnnotation customConfig) {
    if (object instanceof MessageLite.Builder builder) {
      object = builder.build();
    }
    if (!(object instanceof MessageLite message)) {
      throw new IllegalArgumentException("Object is not a protobuf message");
    }
    return new ProtoByteArray(message.toByteString());
  }

  /**
   * @param payload the payload to deserialize - supported types are byte[], {@link ByteArray},
   *     {@link ByteString}
   * @param typeInfo a protobuf parser for the deserialization type
   * @param customConfig not used
   * @return
   * @param <T>
   */
  @SuppressWarnings("unchecked")
  @Override
  default <T> T deserialize(Object payload, Object typeInfo, @Nullable NoAnnotation customConfig) {
    try {
      if (typeInfo instanceof Parser<?> parser) {
        if (payload instanceof byte[] bytes) {
          return (T) parser.parseFrom(bytes);
        } else if (payload instanceof ProtoByteArray protoByteArray) {
          return (T) parser.parseFrom(protoByteArray.toByteString());
        } else if (payload instanceof ByteString bytesString) {
          return (T) parser.parseFrom(bytesString);
        } else if (payload instanceof ByteArray byteArray) {
          return (T) parser.parseFrom(byteArray.toArray());
        }
      }
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
    throw new IllegalArgumentException(
        "Cannot deserialize payload of type " + payload.getClass() + " for typeInfo " + typeInfo);
  }
}
