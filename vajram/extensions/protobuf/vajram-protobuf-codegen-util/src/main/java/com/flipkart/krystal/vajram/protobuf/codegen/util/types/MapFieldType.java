package com.flipkart.krystal.vajram.protobuf.codegen.util.types;

import static java.util.stream.Collectors.toSet;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.Element;

public record MapFieldType(
    ProtoFieldType keyType, ProtoFieldType valueType, CodeGenUtility util, Element element)
    implements ProtoFieldType {

  public MapFieldType {
    if (!keyType.canBeMapKey()) {
      throw new IllegalArgumentException(
          String.format(
              "The type %s is not allowed in a map key. Only integral and string types are allowed.",
              keyType));
    }
    if (!valueType.canBeMapValue()) {
      util.error(String.format("The type %s is not allowed in a map value.", valueType), element);
    }
  }

  @Override
  public String typeInProtoFile(String fieldContainingPackage) {
    return "map<"
        + keyType.typeInProtoFile(fieldContainingPackage)
        + ", "
        + valueType.typeInProtoFile(fieldContainingPackage)
        + ">";
  }

  @Override
  public boolean canBeMapValue() {
    return false;
  }

  @Override
  public String toString() {
    return "map<" + keyType + ", " + valueType + ">";
  }

  @Override
  public Set<String> imports() {
    return Stream.concat(keyType.imports().stream(), valueType.imports().stream()).collect(toSet());
  }
}
