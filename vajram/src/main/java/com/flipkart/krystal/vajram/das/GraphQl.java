package com.flipkart.krystal.vajram.das;

import java.util.Collection;

public final class GraphQl implements DataAccessSpec {

  @Override
  public <T> T adapt(Collection<T> dataObjects) {
    // TODO merge multiple graphQL results of the same type to a single result.
    return null;
  }
}
