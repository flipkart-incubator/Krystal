package com.flipkart.krystal.vajram.das;

import com.flipkart.krystal.vajram.Vajram;

public sealed interface AccessSpecIndex<T extends DataAccessSpec>
    permits VajramIDIndex, GraphQlIndex {
  AccessSpecMatchingResult<T> getVajrams(T accessSpec);

  void add(Vajram vajram);
}
