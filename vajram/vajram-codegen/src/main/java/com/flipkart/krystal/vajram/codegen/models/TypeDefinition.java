package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public sealed class TypeDefinition permits AbstractInput {
  private TypeOfData dataType;
  private List<String> customDataType = new ArrayList<>();
  private List<TypeDefinition> typeParameters = new ArrayList<>();

  public DataType toDataType() {
    List<? extends DataType> typeParameterDataTypes =
        typeParameters.stream().map(TypeDefinition::toDataType).toList();
    return dataType.toDataType(customDataType, typeParameterDataTypes);
  }
}
