package com.flipkart.krystal.vajram.protobuf3.codegen.types;

import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import javax.lang.model.element.Element;
import lombok.ToString;

public record RepeatedFieldType(ProtoFieldType elementType, Utils util, Element element)
    implements ProtoFieldType {

  public RepeatedFieldType {
    if (!elementType.canRepeat()) {
      throw util.errorAndThrow(elementType + " cannot be a repeated field", element);
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
