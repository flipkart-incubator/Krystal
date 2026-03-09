package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.MODELS_PROTO_FILE_SUFFIX;

import java.util.List;

public record MessageFieldType(String packageName, String messageName, String fileName)
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
    return List.of(packageName.replace('.', '/') + "/" + fileName + MODELS_PROTO_FILE_SUFFIX);
  }
}
