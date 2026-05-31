package com.flipkart.krystal.vajram.protobuf.codegen.util.types;

import java.util.Set;

/**
 * Represents an enum field type in a protobuf schema. Similar to {@link MessageFieldType} but for
 * enum types rather than message types.
 */
public record EnumFieldType(String packageName, String enumName, String fileName, String fileSuffix)
    implements ProtoFieldType {

  @Override
  public String typeInProtoFile(String fieldContainingPackage) {
    if (!fieldContainingPackage.equals(packageName)) {
      return packageName + "." + enumName;
    }
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
  public Set<String> imports() {
    return Set.of(packageName.replace('.', '/') + "/" + fileName + fileSuffix);
  }
}
