package com.flipkart.krystal.vajram.graphql.codegen;

public record GraphQLTypeName(String value) {
  public static GraphQLTypeName of(String entityType) {
    return new GraphQLTypeName(entityType);
  }
}
