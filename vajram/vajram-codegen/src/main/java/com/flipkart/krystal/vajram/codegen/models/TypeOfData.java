package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.BooleanType;
import com.flipkart.krystal.datatypes.CustomType;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.IntegerType;
import com.flipkart.krystal.datatypes.ListType;
import com.flipkart.krystal.datatypes.SetType;
import com.flipkart.krystal.datatypes.StringType;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public enum TypeOfData {
  bool(BooleanType::bool),
  integer(IntegerType::integer),
  string(StringType::string),
  set(
      typeParameters -> {
        checkCount(1, typeParameters, "Set");
        return SetType.set(typeParameters.get(0));
      }),
  list(
      typeParameters -> {
        checkCount(1, typeParameters, "List");
        return ListType.list(typeParameters.get(0));
      }),
  custom_java(
      (customTypeInfo, typeParameters) -> CustomType.create(customTypeInfo, typeParameters)),
  ;

  private final BiFunction<List<String>, List<? extends DataType<?>>, DataType<?>> dataTypeSupplier;

  TypeOfData(Supplier<DataType<?>> typeParametersToDataType) {
    this.dataTypeSupplier = (strings, dataTypes) -> typeParametersToDataType.get();
  }

  TypeOfData(Function<List<? extends DataType<?>>, DataType<?>> typeParametersToDataType) {
    this.dataTypeSupplier = (strings, dataTypes) -> typeParametersToDataType.apply(dataTypes);
  }

  TypeOfData(
      BiFunction<List<String>, List<? extends DataType<?>>, DataType<?>> typeParametersToDataType) {
    this.dataTypeSupplier = typeParametersToDataType;
  }

  public DataType<?> toDataType(
      List<String> customTypeInfo, List<? extends DataType<?>> typeParameters) {
    return dataTypeSupplier.apply(customTypeInfo, typeParameters);
  }

  private static void checkCount(
      int desiredCount, List<? extends DataType<?>> dataTypes, String typeOfData) {
    if (dataTypes.size() != desiredCount) {
      throw new IllegalArgumentException(
          "%s Type can have only one parameterType".formatted(typeOfData));
    }
  }
}
