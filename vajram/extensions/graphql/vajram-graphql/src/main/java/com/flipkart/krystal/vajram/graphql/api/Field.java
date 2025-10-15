package com.flipkart.krystal.vajram.graphql.api;

import lombok.Getter;

@Getter
public abstract class Field {

  protected final String fieldName;
  protected final String fieldPath;
  protected final String resolverId;
  protected final String identifierKeyForResolver;
  protected final GraphQLFieldType fieldType;
  protected final Type type;

  public Field(
      String fieldName,
      String fieldPath,
      String resolverId,
      String identifierKeyForResolver,
      GraphQLFieldType fieldType,
      Type type) {
    this.fieldName = fieldName;
    this.fieldPath = fieldPath;
    this.resolverId = resolverId;
    this.identifierKeyForResolver = identifierKeyForResolver;
    this.fieldType = fieldType;
    this.type = type;
  }

  public void addSubField(Field field) {
    this.type.fields().add(field);
  }

  public abstract String getRefFetcherId();
}
