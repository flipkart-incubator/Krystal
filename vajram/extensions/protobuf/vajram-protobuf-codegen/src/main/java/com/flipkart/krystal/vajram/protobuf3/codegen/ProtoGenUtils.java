package com.flipkart.krystal.vajram.protobuf3.codegen;

import static lombok.AccessLevel.PRIVATE;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

@NoArgsConstructor(access = PRIVATE)
public class ProtoGenUtils {

  static @NonNull String javaPackageToProtoPackageName(String packageName) {
    return packageName;
  }

  static @NonNull String getSimpleClassName(String canonicalClassName) {
    String typeName = canonicalClassName;

    // Extract the simple name from the fully qualified name
    if (typeName.contains(".")) {
      typeName = typeName.substring(typeName.lastIndexOf('.') + 1);
    }

    // Add the _Proto suffix
    return typeName;
  }

  static @NonNull Optional<String> getPackageName(String responseTypeName) {
    int lastDotIndex = responseTypeName.lastIndexOf('.');
    if (lastDotIndex == -1) {
      return Optional.empty();
    }
    return Optional.of(responseTypeName.substring(0, lastDotIndex));
  }
}
