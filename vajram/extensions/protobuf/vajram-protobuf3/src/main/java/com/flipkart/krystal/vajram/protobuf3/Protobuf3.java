package com.flipkart.krystal.vajram.protobuf3;

import com.flipkart.krystal.vajram.protobuf.util.ProtobufProtocol;

public final class Protobuf3 implements ProtobufProtocol {

  public static final Protobuf3 PROTOBUF_3 = new Protobuf3();

  private Protobuf3() {}

  @Override
  public String modelClassesSuffix() {
    return "Proto3";
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
    return "syntax = \"proto3\";";
  }

  @Override
  public String protoFileSuffix() {
    return ".models.proto3.proto";
  }
}
