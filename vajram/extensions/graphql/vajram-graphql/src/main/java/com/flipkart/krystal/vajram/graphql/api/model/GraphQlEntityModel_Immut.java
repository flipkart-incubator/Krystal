package com.flipkart.krystal.vajram.graphql.api.model;

public interface GraphQlEntityModel_Immut<I extends GraphQlEntityId>
    extends GraphQlEntityModel<I>, GraphQlTypeModel_Immut {
  interface Builder<I extends GraphQlEntityId> extends GraphQlTypeModel_Immut.Builder {
    I id();
  }
}
