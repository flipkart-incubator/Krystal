package com.flipkart.krystal.vajram.graphql.api;

import static com.flipkart.krystal.vajram.graphql.api.GraphQLUtils.handleErrable;

import com.flipkart.krystal.data.Errable;

public abstract class AbstractGraphQLEntity<
        I extends GraphQLEntityId, T extends AbstractGraphQLEntity<I, T>>
    extends AbstractGraphQlModel<T> {

  public static final String DEFAULT_ENTITY_ID_FIELD = "id";

  public abstract String __typename();
}
