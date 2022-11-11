package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.das.DataAccessSpec;
import java.util.Collection;

public record VajramID(String vajramId) implements DataAccessSpec {
  public static VajramID vajramID(String id) {
    return new VajramID(id);
  }

  @Override
  public <T> T adapt(Collection<T> dataObjects) {
    throw new UnsupportedOperationException("");
  }
}
