package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.graphql.codegen.WrappedType.WrapperType.LIST;
import static com.flipkart.krystal.vajram.graphql.codegen.WrappedType.WrapperType.NONNULL;

import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.Type;
import lombok.Value;

@Value
public class WrappedType implements GraphQlTypeDecorator {
  public enum WrapperType {
    NONNULL,
    LIST
  }

  Type graphQlType;
  WrapperType wrapperType;
  GraphQlTypeDecorator innerType;

  public WrappedType(Type graphQlType) {
    this.graphQlType = graphQlType;
    if (graphQlType instanceof NonNullType nonNullType) {
      wrapperType = WrapperType.NONNULL;
      innerType = GraphQlTypeDecorator.of(nonNullType.getType());
      if (innerType.isNonNull()) {
        throw new IllegalArgumentException("A NonNull type cannot wrap another NonNull type");
      }
    } else if (graphQlType instanceof ListType listType) {
      wrapperType = LIST;
      innerType = GraphQlTypeDecorator.of(listType.getType());
    } else {
      throw new IllegalArgumentException(
          "Only NonNullType and ListType are allowed as wrapper types. Found: " + graphQlType);
    }
  }

  @Override
  public boolean isList() {
    return wrapperType == LIST;
  }

  @Override
  public boolean isNonNull() {
    return wrapperType == NONNULL;
  }
}
