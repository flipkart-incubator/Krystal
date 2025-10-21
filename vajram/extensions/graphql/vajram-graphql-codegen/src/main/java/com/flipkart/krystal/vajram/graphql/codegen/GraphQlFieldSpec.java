package com.flipkart.krystal.vajram.graphql.codegen;

import com.squareup.javapoet.TypeName;
import graphql.language.FieldDefinition;
import lombok.Builder;

@Builder
public record GraphQlFieldSpec(
    String fieldName, FieldType fieldType, FieldDefinition fieldDefinition) {

  interface FieldType {
    TypeName genericType();

    TypeName declaredType();

    boolean isList();
  }

  record ListFieldType(TypeName genericType, TypeName declaredType) implements FieldType {

    @Override
    public boolean isList() {
      return true;
    }
  }

  record SingleFieldType(TypeName declaredType) implements FieldType {

    @Override
    public TypeName genericType() {
      return declaredType;
    }

    @Override
    public boolean isList() {
      return false;
    }
  }
}
