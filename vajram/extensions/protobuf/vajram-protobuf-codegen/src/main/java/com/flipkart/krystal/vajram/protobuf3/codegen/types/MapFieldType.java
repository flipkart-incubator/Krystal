package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import javax.lang.model.element.Element;

public record MapFieldType(
    ProtoFieldType keyType, ProtoFieldType valueType, Utils util, Element element)
    implements ProtoFieldType {

  public MapFieldType {
    if (!keyType.canBeMapKey()) {
      throw new IllegalArgumentException(
          String.format(
              "The type %s is not allowed in a map key. Only integral and string types are allowed.",
              keyType));
    }
    if (!valueType.canBeMapValue()) {
      throw util.errorAndThrow(
          String.format("The type %s is not allowed in a map value.", valueType), element);
    }
  }

  @Override
  public String typeInProtoFile() {
    return "map <" + keyType.typeInProtoFile() + ", " + valueType.typeInProtoFile() + ">";
  }

  @Override
  public boolean canBeMapValue() {
    return false;
  }
  @Override
  public String toString() {
    return typeInProtoFile();
  }
}
