package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public sealed class TypeDefinition permits AbstractInput {
  private TypeOfData dataType;
  private List<String> customDataType = new ArrayList<>();
  private List<TypeDefinition> typeParameters = new ArrayList<>();

  public static TypeDefinition ofCustom(Class<?> klass) {
    return new TypeDefinition(TypeOfData.custom_java, List.of(klass.getTypeName()), List.of());
  }

  public DataType<?> toDataType() {
    return dataType.toDataType(
        customDataType, typeParameters.stream().map(TypeDefinition::toDataType).toList());
  }

  public TypeName toTypeName() {
    return dataType.toTypeName(
        customDataType,
        typeParameters.stream().map(TypeDefinition::toTypeName).toArray(TypeName[]::new));
  }
}
