package com.flipkart.krystal.vajram.protobuf.codegen.util.types;

import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface ProtoFieldType {
  String typeInProtoFile(String fieldContainingPackage);

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

  default @Nullable String packageName() {
    return null;
  }

  default Set<String> imports() {
    return Set.of();
  }
}
