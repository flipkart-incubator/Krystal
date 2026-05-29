package com.flipkart.krystal.vajram.protobuf.codegen.util.types;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import java.util.Set;
import javax.lang.model.element.Element;

public record OptionalFieldType(ProtoFieldType elementType, CodeGenUtility util, Element element)
    implements ProtoFieldType {

  public OptionalFieldType {
    if (!elementType.canBeOptional()) {
      util.error(elementType + " cannot be marked optional", element);
    }
  }

  @Override
  public String typeInProtoFile(String fieldContainingPackage) {
    return "optional " + elementType.typeInProtoFile(fieldContainingPackage);
  }

  @Override
  public boolean canBeMapValue() {
    return false;
  }

  @Override
  public String toString() {
    return "optional " + elementType;
  }

  @Override
  public Set<String> imports() {
    return elementType.imports();
  }
}
