package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.BooleanType;
import com.flipkart.krystal.datatypes.CustomType;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.IntegerType;
import com.flipkart.krystal.datatypes.ListType;
import com.flipkart.krystal.datatypes.SetType;
import com.flipkart.krystal.datatypes.StringType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public enum TypeOfData {
  bool(BooleanType::bool, () -> TypeName.BOOLEAN),
  integer(IntegerType::integer, () -> TypeName.INT),
  string(StringType::string, () -> TypeName.get(String.class)),

  set(
      typeParameters -> {
        checkCount(1, typeParameters.size(), "Set");
        return SetType.set(typeParameters.get(0));
      },
      typeParameters -> {
        checkCount(1, typeParameters.length, "Set");
        return ParameterizedTypeName.get(ClassName.get(SetType.COLLECTION_TYPE), typeParameters);
      }),

  list(
      typeParameters -> {
        checkCount(1, typeParameters.size(), "List");
        return ListType.list(typeParameters.get(0));
      },
      typeParameters -> {
        checkCount(1, typeParameters.length, "List");
        return ParameterizedTypeName.get(ClassName.get(ListType.COLLECTION_TYPE), typeParameters);
      }),

  custom_java(
      CustomType::create,
      (customTypeInfo, typeParameters) -> {
        ClassName className = toClassName(customTypeInfo);
        if (typeParameters.length == 0) {
          return className;
        }
        return ParameterizedTypeName.get(className, typeParameters);
      }),
  ;

  private final BiFunction<List<String>, List<? extends DataType<?>>, DataType<?>> dataTypeSupplier;
  private final BiFunction<List<String>, TypeName[], TypeName> typeNameSupplier;

  TypeOfData(
      Supplier<DataType<?>> typeParametersToDataType, Supplier<TypeName> typeParametersToTypeName) {
    this.dataTypeSupplier = (strings, dataTypes) -> typeParametersToDataType.get();
    this.typeNameSupplier = (strings, dataTypes) -> typeParametersToTypeName.get();
  }

  TypeOfData(
      Function<List<? extends DataType<?>>, DataType<?>> typeParametersToDataType,
      Function<TypeName[], TypeName> typeParametersToTypeName) {
    this.dataTypeSupplier = (strings, dataTypes) -> typeParametersToDataType.apply(dataTypes);
    this.typeNameSupplier = (strings, dataTypes) -> typeParametersToTypeName.apply(dataTypes);
  }

  TypeOfData(
      BiFunction<List<String>, List<? extends DataType<?>>, DataType<?>> typeParametersToDataType,
      BiFunction<List<String>, TypeName[], TypeName> typeParametersToTypeName) {
    this.dataTypeSupplier = typeParametersToDataType;
    this.typeNameSupplier = typeParametersToTypeName;
  }

  public DataType<?> toDataType(
      List<String> customTypeInfo, List<? extends DataType<?>> typeParameters) {
    return dataTypeSupplier.apply(customTypeInfo, typeParameters);
  }

  public TypeName toTypeName(List<String> customTypeInfo, TypeName[] typeParameters) {
    return typeNameSupplier.apply(customTypeInfo, typeParameters);
  }

  private static ClassName toClassName(List<String> typeInfo) {
    if (typeInfo.isEmpty()) {
      throw new IllegalArgumentException("At least one type name is needed for java types.");
    }
    if (typeInfo.size() == 1) {
      String className = typeInfo.get(0);
      int lastDot = className.lastIndexOf('.');
      return ClassName.get(className.substring(0, lastDot), className.substring(lastDot + 1));
    }
    return ClassName.get(
        typeInfo.get(0),
        typeInfo.get(typeInfo.size() - 1),
        typeInfo.subList(1, typeInfo.size() - 1).toArray(String[]::new));
  }

  private static void checkCount(int desiredCount, int actualCount, String typeOfData) {
    if (actualCount != desiredCount) {
      throw new IllegalArgumentException(
          "%s Type can have only one parameterType".formatted(typeOfData));
    }
  }
}
