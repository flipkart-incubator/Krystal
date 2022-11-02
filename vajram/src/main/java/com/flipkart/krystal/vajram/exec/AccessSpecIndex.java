package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.google.common.collect.ImmutableMap;

sealed interface AccessSpecIndex<T extends DataAccessSpec> permits VajramIDIndex, GraphQlIndex {
  public ImmutableMap<T, Vajram<?>> getVajrams(T accessSpec);

  void add(Vajram<?> vajram);
}
