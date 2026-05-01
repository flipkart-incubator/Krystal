package com.flipkart.krystal.vajram.protobuf.util;

import com.flipkart.krystal.serial.SerdeProtocol;

/**
 * Marker interface for SerdeProtocols that represent a protobuf flavour (proto3, edition 2024,
 * etc.). Allows protocol-agnostic codegen modules (e.g. lattice-grpc-codegen) to identify and work
 * with any supported protobuf protocol without depending on each protobuf module directly.
 *
 * <p>Each implementation also exposes the protocol-specific schema strings (header line, generated
 * file suffix) so downstream codegen can compose schemas without taking a hard dependency on a
 * particular protobuf module.
 */
public interface ProtobufProtocol extends SerdeProtocol {

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
}
