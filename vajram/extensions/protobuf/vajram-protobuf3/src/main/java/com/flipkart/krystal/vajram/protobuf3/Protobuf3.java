package com.flipkart.krystal.vajram.protobuf3;

import com.flipkart.krystal.serial.SerdeProtocol;

public final class Protobuf3 implements SerdeProtocol {

  public static final Protobuf3 PROTOBUF_3 = new Protobuf3();

  private Protobuf3() {}

  @Override
  public String modelClassesSuffix() {
    return "Proto";
  }

  @Override
  public String defaultContentType() {
    return "application/protobuf";
  }
}
