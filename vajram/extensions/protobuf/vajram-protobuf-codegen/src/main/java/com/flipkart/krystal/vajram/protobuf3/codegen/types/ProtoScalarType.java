package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import lombok.Getter;

public enum ProtoScalarType implements ProtoFieldType {
  BOOL_P("bool", true),

  SINT32_P("sint32", true),
  SINT64_P("sint64", true),

  FLOAT_P("float", false),
  DOUBLE_P("double", false),

  STRING_P("string", true),
  BYTES_P("bytes", false);

  @Getter private final String typeInProtoFile;
  @Getter private final boolean canBeMapKey;

  ProtoScalarType(String typeInProtoFile, boolean canBeMapKey) {
    this.typeInProtoFile = typeInProtoFile;
    this.canBeMapKey = canBeMapKey;
  }

  @Override
  public boolean canRepeat() {
    return true;
  }

  @Override
  public boolean canBeOptional() {
    return true;
  }

  @Override
  public String toString() {
    return typeInProtoFile();
  }
}
