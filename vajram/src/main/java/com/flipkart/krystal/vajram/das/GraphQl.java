package com.flipkart.krystal.vajram.das;

import java.util.Collection;

public final class GraphQl implements DataAccessSpec {

  @Override
  public <T> T merge(Collection<T> graphQlResults) {
    // TODO merge multiple graphQL results of the same type to a single result.
    return null;
  }
}
