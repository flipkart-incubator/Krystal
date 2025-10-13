package com.flipkart.krystal.vajram.graphql.codegen;

public record EntityTypeName(String value) {
  public static EntityTypeName of(String entityType) {
    return new EntityTypeName(entityType);
  }
}