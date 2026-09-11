package com.flipkart.krystal.krystex.epochs;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.google.common.collect.ImmutableSet;

public record EpochGroup(
    VajramID vajramID, int globalEpoch, ImmutableSet<DependentChain> dependentChains) {

  public static EpochGroup newDepChainEpochGroup(
      VajramID vajramID, int globalEpoch, DependentChain... dependentChains) {
    return new EpochGroup(vajramID, globalEpoch, ImmutableSet.copyOf(dependentChains));
  }
}
