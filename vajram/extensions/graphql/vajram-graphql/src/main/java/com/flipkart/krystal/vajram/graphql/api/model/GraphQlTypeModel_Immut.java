package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.ImmutableModel;

public interface GraphQlTypeModel_Immut extends GraphQlTypeModel, ImmutableModel {
  interface Builder extends GraphQlTypeModel, ImmutableModel.Builder {
    /**
     * Sets or unsets the __typename of a graphql type. The exact value of the __typename that is
     * set is an implementation detail and is not controlled by the caller
     *
     * @param set true if __typename should be set, false if it should be unset (set to null)
     */
    void __typename(boolean set);
  }
}
