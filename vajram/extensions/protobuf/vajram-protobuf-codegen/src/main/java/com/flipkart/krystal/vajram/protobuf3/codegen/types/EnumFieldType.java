package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import static com.flipkart.krystal.vajram.protobuf3.codegen.VajramProtoConstants.MODELS_PROTO_FILE_SUFFIX;

import java.util.List;

/**
 * Represents a proto3 enum field type. Similar to {@link MessageFieldType} but for enum types
 * rather than message types.
 */
public record EnumFieldType(String packageName, String enumName, String fileName)
    implements ProtoFieldType {

  @Override
  public String typeInProtoFile() {
    return enumName;
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
  public boolean canBeMapValue() {
    return true;
  }

  @Override
  public List<String> imports() {
    return List.of(packageName.replace('.', '/') + "/" + fileName + MODELS_PROTO_FILE_SUFFIX);
  }
}
