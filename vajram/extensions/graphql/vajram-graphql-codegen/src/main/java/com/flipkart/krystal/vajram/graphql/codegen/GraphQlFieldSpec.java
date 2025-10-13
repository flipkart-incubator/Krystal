package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.fkEntities.typeAggregatorGenerator.TypeAggregatorAutoGenerator.PACKAGE_NAME_ENTITY;

import com.squareup.javapoet.TypeName;
import graphql.language.FieldDefinition;
import lombok.Builder;

@Builder
public record GraphQlFieldSpec(String fieldName, TypeName fieldType) {

  public static GraphQlFieldSpec fromField(FieldDefinition fieldDefinition) {
    return GraphQlFieldSpec.builder()
        .fieldName(fieldDefinition.getName())
        .fieldType(SchemaReaderUtil.getFieldType(fieldDefinition))
        .build();
  }
}