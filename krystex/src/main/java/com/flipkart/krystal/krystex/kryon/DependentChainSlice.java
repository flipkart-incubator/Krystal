package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;

public sealed interface DependentChainSlice extends DependentChainBase
    permits DependentChainSliceImpl {
  @Override
  DependentChainSlice extend(VajramID vajramID, Dependency dependency);

  static DependentChainSlice newStartingFrom(VajramID vajramID, Dependency dependency) {
    return new DependentChainSliceImpl(vajramID, dependency, null);
  }
}
