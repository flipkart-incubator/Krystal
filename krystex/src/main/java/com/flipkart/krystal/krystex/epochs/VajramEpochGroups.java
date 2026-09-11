package com.flipkart.krystal.krystex.epochs;

import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

public class VajramEpochGroups {
  @Getter private final ImmutableMap<Integer, EpochGroup> depChainsByEpochGroup;
  @Getter private final ImmutableMap<DependentChain, Integer> epochByDepChains;

  public VajramEpochGroups(ImmutableMap<Integer, EpochGroup> depChainsByEpochGroup) {
    this.depChainsByEpochGroup = depChainsByEpochGroup;
    this.epochByDepChains = compute(depChainsByEpochGroup);
  }

  private ImmutableMap<DependentChain, Integer> compute(
      ImmutableMap<Integer, EpochGroup> depChainsByEpochGroup) {
    Map<DependentChain, Integer> epochByDepChains = new LinkedHashMap<>();
    depChainsByEpochGroup.forEach(
        (epoch, depChainEpochGroup) -> {
          for (DependentChain dependentChain : depChainEpochGroup.dependentChains()) {
            epochByDepChains.put(dependentChain, epoch);
          }
        });
    return ImmutableMap.copyOf(epochByDepChains);
  }
}
