package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.das.DataAccessSpec;

sealed interface AccessSpecIndex<T extends DataAccessSpec> permits VajramIDIndex, GraphQlIndex {
  AccessSpecMatchingResult<T> getVajrams(T accessSpec);

  void add(Vajram<?> vajram);
}
