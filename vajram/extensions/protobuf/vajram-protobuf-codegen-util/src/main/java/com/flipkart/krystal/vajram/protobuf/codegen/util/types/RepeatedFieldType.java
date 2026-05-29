package com.flipkart.krystal.vajram.protobuf.codegen.util.types;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import java.util.Set;
import javax.lang.model.element.Element;
import org.checkerframework.checker.nullness.qual.Nullable;

public record RepeatedFieldType(ProtoFieldType elementType, CodeGenUtility util, Element element)
    implements ProtoFieldType {

  public RepeatedFieldType {
    if (!elementType.canRepeat()) {
      util.error(elementType + " cannot be a repeated field", element);
    }
  }

  @Override
  public String typeInProtoFile(String fieldContainingPackage) {
    return "repeated " + elementType.typeInProtoFile(fieldContainingPackage);
  }

  @Override
  public boolean canBeMapValue() {
    return false;
  }

  @Override
  public String toString() {
    return "repeated " + elementType;
  }

  @Override
  public @Nullable String packageName() {
    return elementType.packageName();
  }

  @Override
  public Set<String> imports() {
    return elementType.imports();
  }
}
