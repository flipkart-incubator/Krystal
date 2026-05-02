package com.flipkart.krystal.vajram.protobuf2024e;

import com.flipkart.krystal.vajram.protobuf.util.ProtobufProtocol;

/**
 * SerdeProtocol for protobuf edition 2024.
 *
 * <p>Edition 2024 is the current released edition of Protocol Buffers and supersedes proto3 as the
 * recommended way to author new schemas. The wire format is the same as proto3, so there is no
 * runtime cost; the differences are at the schema level (explicit field presence by default, open
 * enums by default, packed repeated fields by default, UTF-8 validation, JSON-format allowed,
 * length-prefixed message encoding).
 *
 * <p>As the latest protobuf protocol supported by Krystal, this protocol claims the canonical
 * "Proto" suffix for generated wrapper class names ({@code Foo_ImmutProto}). Models that opt into
 * the older proto3 protocol get a "Proto3" suffix instead.
 */
public final class Protobuf2024e implements ProtobufProtocol {

  public static final Protobuf2024e PROTOBUF_2024E = new Protobuf2024e();

  private Protobuf2024e() {}

  @Override
  public String modelClassesSuffix() {
    return "Proto";
  }

  @Override
  public String defaultContentType() {
    return "application/protobuf";
  }

  @Override
  public boolean modelsNeedToBePure() {
    return true;
  }

  @Override
  public String schemaHeader() {
    return "edition = \"2024\";";
  }

  @Override
  public String protoFileSuffix() {
    return ".models.proto";
  }

  @Override
  public boolean emitJavaMultipleFiles() {
    return false;
  }
}
