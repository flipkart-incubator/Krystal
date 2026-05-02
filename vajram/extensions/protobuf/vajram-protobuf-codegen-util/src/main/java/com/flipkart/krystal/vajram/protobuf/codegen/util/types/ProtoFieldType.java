package com.flipkart.krystal.vajram.protobuf.codegen.util.types;

import java.util.List;

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

  default List<String> imports() {
    return List.of();
  }
}
