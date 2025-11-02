package com.flipkart.krystal.vajram.graphql.api.model;

public interface GraphQlEntityModel_Immut<I extends GraphqlEntityId>
    extends GraphQlEntityModel<I>, GraphQlTypeModel_Immut {
  interface Builder<I extends GraphqlEntityId>
      extends GraphQlEntityModel<I>, GraphQlTypeModel_Immut.Builder {}
}
