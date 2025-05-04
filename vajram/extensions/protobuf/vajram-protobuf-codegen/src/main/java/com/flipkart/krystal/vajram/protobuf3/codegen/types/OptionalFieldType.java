package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility;
import javax.lang.model.element.Element;

public record OptionalFieldType(ProtoFieldType elementType, CodeGenUtility util, Element element)
    implements ProtoFieldType {

  public OptionalFieldType {
    if (!elementType.canBeOptional()) {
      util.error(elementType + " cannot be marked optional", element);
    }
  }

  @Override
  public String typeInProtoFile() {

    return "optional " + elementType.typeInProtoFile();
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
