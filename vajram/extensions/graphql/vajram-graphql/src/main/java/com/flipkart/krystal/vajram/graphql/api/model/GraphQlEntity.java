package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.ModelClusterRoot;

@ModelClusterRoot(
    immutableRoot = GraphQlEntity_Immut.class,
    builderRoot = GraphQlEntity_Immut.Builder.class)
public interface GraphQlEntity<I extends GraphQlEntityId> extends GraphQlObject {
  I id();
}
