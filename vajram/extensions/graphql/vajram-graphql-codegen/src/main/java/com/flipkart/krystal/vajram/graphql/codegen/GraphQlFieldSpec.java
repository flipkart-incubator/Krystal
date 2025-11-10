package com.flipkart.krystal.vajram.graphql.codegen;

import graphql.language.FieldDefinition;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public record GraphQlFieldSpec(
    String fieldName,
    GraphQlTypeDecorator fieldType,
    FieldDefinition fieldDefinition,
    @Nullable GraphQLTypeName enclosingType) {

  @Builder
  public GraphQlFieldSpec(
      @NonNull String fieldName,
      @NonNull FieldDefinition fieldDefinition,
      @NonNull GraphQLTypeName enclosingType) {
    this(
        fieldName,
        GraphQlTypeDecorator.of(fieldDefinition.getType()),
        fieldDefinition,
        enclosingType);
  }
}
