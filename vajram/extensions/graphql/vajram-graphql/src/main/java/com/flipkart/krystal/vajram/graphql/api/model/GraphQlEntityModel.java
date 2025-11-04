package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.ModelClusterRoot;

@ModelClusterRoot(
    immutableRoot = GraphQlEntityModel_Immut.class,
    builderRoot = GraphQlEntityModel_Immut.Builder.class)
public interface GraphQlEntityModel<I extends GraphQlEntityId> extends GraphQlTypeModel {
  I id();
}
