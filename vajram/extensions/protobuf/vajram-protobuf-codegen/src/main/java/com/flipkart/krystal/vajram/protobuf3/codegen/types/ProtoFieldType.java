package com.flipkart.krystal.vajram.protobuf3.codegen.types;

public interface ProtoFieldType {
  String typeInProtoFile();

  default boolean canBeMapKey() {
    return false;
  }

  default boolean canBeMapValue() {
    return true;
  }

  default boolean canRepeat() {
    return false;
  }

  default boolean canBeOptional() {
    return false;
  }
}
