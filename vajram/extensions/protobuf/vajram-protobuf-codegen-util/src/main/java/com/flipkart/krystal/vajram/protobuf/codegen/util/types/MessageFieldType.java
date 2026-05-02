package com.flipkart.krystal.vajram.protobuf.codegen.util.types;

import java.util.List;

public record MessageFieldType(
    String packageName, String messageName, String fileName, String fileSuffix)
    implements ProtoFieldType {

  @Override
  public String typeInProtoFile() {
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
  public List<String> imports() {
    return List.of(packageName.replace('.', '/') + "/" + fileName + fileSuffix);
  }
}
