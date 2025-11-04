package com.flipkart.krystal.vajram.graphql.api;

import com.flipkart.krystal.model.ModelClusterRoot;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlEntityId;

@ModelClusterRoot
public abstract class AbstractGraphQLEntity<
        I extends GraphQlEntityId, T extends AbstractGraphQLEntity<I, T>>
    extends AbstractGraphQlModel<T> {

  public static final String DEFAULT_ENTITY_ID_FIELD = "id";

  public abstract String __typename();
}
