package com.flipkart.krystal.vajram.graphql.codegen;

import graphql.language.TypeDefinition;

public record GraphQLTypeName(String value) {
  public static GraphQLTypeName of(TypeDefinition<?> typeDefinition) {
    return new GraphQLTypeName(typeDefinition.getName());
  }

  public static GraphQLTypeName of(String entityType) {
    return new GraphQLTypeName(entityType);
  }
}
