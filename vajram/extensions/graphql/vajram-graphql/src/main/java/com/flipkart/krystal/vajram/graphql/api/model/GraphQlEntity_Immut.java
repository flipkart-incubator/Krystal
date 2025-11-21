package com.flipkart.krystal.vajram.graphql.api.model;

public interface GraphQlEntity_Immut<I extends GraphQlEntityId>
    extends GraphQlEntity<I>, GraphQlObject_Immut {
  interface Builder<I extends GraphQlEntityId> extends GraphQlObject_Immut.Builder {}
}
