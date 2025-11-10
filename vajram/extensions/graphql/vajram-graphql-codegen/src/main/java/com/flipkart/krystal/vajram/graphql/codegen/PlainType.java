package com.flipkart.krystal.vajram.graphql.codegen;

import graphql.language.TypeName;
import lombok.Value;

@Value
public class PlainType implements GraphQlTypeDecorator {
  TypeName graphQlType;

  @Override
  public GraphQlTypeDecorator innerType() {
    return this;
  }

  @Override
  public boolean isList() {
    return false;
  }

  @Override
  public boolean isNonNull() {
    return false;
  }
}
