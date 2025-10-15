package com.flipkart.krystal.vajram.graphql.codegen;

import com.squareup.javapoet.TypeName;
import graphql.language.FieldDefinition;
import lombok.Builder;

@Builder
public record GraphQlFieldSpec(
    String fieldName, TypeName fieldType, FieldDefinition fieldDefinition) {}
