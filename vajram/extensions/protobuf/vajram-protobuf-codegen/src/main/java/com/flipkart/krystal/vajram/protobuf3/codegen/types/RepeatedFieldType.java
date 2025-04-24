package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import javax.lang.model.element.Element;

public record RepeatedFieldType(ProtoFieldType elementType, Utils util, Element element)
    implements ProtoFieldType {

  public RepeatedFieldType {
    if (!elementType.canRepeat()) {
      util.error(elementType + " cannot be a repeated field", element);
    }
  }

  @Override
  public String typeInProtoFile() {
    return "repeated " + elementType.typeInProtoFile();
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
