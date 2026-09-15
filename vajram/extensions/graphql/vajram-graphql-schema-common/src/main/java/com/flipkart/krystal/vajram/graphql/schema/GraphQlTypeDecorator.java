package com.flipkart.krystal.vajram.graphql.schema;

import graphql.language.Type;
import graphql.language.TypeName;

public sealed interface GraphQlTypeDecorator permits PlainType, WrappedType {

  static GraphQlTypeDecorator of(Type<?> graphQlType) {
    if (graphQlType instanceof TypeName typeName) {
      return new PlainType(typeName);
    } else {
      return new WrappedType(graphQlType);
    }
  }

  Type<?> graphQlType();

  GraphQlTypeDecorator innerType();

  boolean isList();

  boolean isNonNull();
}
