package com.flipkart.krystal.vajram.graphql.api;

import static com.flipkart.krystal.vajram.graphql.api.GraphQLUtils.handleErrable;

import com.flipkart.krystal.data.Errable;

public abstract class AbstractGraphQLEntity<
        I extends GraphQLEntityId, T extends AbstractGraphQLEntity<I, T>>
    extends AbstractGraphQlModel<T> {
  @SuppressWarnings("unchecked")
  public final I id() {
    return (I) _values.get("id");
  }

  public final void id(I id) {
    handleErrable("id", Errable.withValue(id), this);
  }

  public abstract String __typename();
}
