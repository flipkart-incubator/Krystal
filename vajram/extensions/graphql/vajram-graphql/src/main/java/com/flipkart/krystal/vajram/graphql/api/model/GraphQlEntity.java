package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.ModelClusterRoot;

@ModelClusterRoot(
    immutableRoot = GraphQlEntity_Immut.class,
    builderRoot = GraphQlEntity_Immut.Builder.class)
public interface GraphQlEntity<I extends GraphQlEntityId> extends GraphQlObject {
  // No fixed id() method - entities define their own ID field method with custom names
}
