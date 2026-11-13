package com.flipkart.krystal.krystex.epochs;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

public class VajramEpochGroups {

  @Getter private final VajramID vajramID;
  @Getter private final ImmutableMap<Integer, EpochGroup> depChainsByEpochs;
  @Getter private final ImmutableMap<DependentChain, Integer> epochByDepChains;
  @Getter private final int maxEpoch;

  public VajramEpochGroups(VajramID vajramID, ImmutableMap<Integer, EpochGroup> depChainsByEpochs) {
    this.vajramID = vajramID;
    this.depChainsByEpochs = depChainsByEpochs;
    this.epochByDepChains = compute(depChainsByEpochs);
    this.maxEpoch = depChainsByEpochs.isEmpty() ? -1 : Collections.max(depChainsByEpochs.keySet());
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
