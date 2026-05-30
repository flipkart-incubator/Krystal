package com.flipkart.krystal.vajram.protobuf.codegen.util.types;

import java.util.Set;

public record MessageFieldType(
    String packageName, String messageName, String fileName, String fileSuffix)
    implements ProtoFieldType {

  @Override
  public String typeInProtoFile(String fieldContainingPackage) {
    if (!fieldContainingPackage.equals(packageName)) {
      return packageName + "." + messageName;
    }
    return messageName;
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
  public Set<String> imports() {
    return Set.of(packageName.replace('.', '/') + "/" + fileName + fileSuffix);
  }
}
