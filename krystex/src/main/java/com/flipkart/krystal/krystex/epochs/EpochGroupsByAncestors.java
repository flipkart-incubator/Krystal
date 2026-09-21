package com.flipkart.krystal.krystex.epochs;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.DependentChainStart;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public record EpochGroupsByAncestors(
    EpochGroups allEpochGroups,
    ImmutableMap<DependentChain, ImmutableMap<VajramID, EpochGroups>> epochGroupsFromVajram) {

  public static EpochGroupsByAncestors empty(DependentChainStart dependentChainStart) {
    return new EpochGroupsByAncestors(
        EpochGroups.empty(dependentChainStart),
        ImmutableMap.<DependentChain, ImmutableMap<VajramID, EpochGroups>>of());
  }

  public EpochGroupsByAncestors(
      EpochGroups allEpochGroups,
      Map<DependentChain, Map<VajramID, Map<VajramID, Map<Integer, Set<DependentChain>>>>>
          epochGroupsByAncestorsCollector) {
    this(allEpochGroups, convert(epochGroupsByAncestorsCollector));
  }

  private static ImmutableMap<DependentChain, ImmutableMap<VajramID, EpochGroups>> convert(
      @MonotonicNonNull
          Map<DependentChain, Map<VajramID, Map<VajramID, Map<Integer, Set<DependentChain>>>>>
              epochGroupsByAncestorsCollector) {
    Map<DependentChain, ImmutableMap<VajramID, EpochGroups>> collector1 = new LinkedHashMap<>();
    epochGroupsByAncestorsCollector.forEach(
        (dependentChain, epochGroupsByOnVajram) -> {
          Map<VajramID, EpochGroups> collector2 = new LinkedHashMap<>();
          epochGroupsByOnVajram.forEach(
              (onVajram, epochGroupsByTarget) -> {
                Map<VajramID, VajramEpochGroups> collector3 = new LinkedHashMap<>();
                epochGroupsByTarget.forEach(
                    (targetVajram, dependentChainsByEpoch) -> {
                      Map<Integer, EpochGroup> collector4 = new LinkedHashMap<>();
                      dependentChainsByEpoch.forEach(
                          (epoch, dependentChains) ->
                              collector4.put(
                                  epoch,
                                  new EpochGroup(
                                      targetVajram, epoch, ImmutableSet.copyOf(dependentChains))));
                      collector3.put(
                          targetVajram,
                          new VajramEpochGroups(targetVajram, ImmutableMap.copyOf(collector4)));
                    });
                collector2.put(
                    onVajram, new EpochGroups(dependentChain, ImmutableMap.copyOf(collector3)));
              });
          collector1.put(dependentChain, ImmutableMap.copyOf(collector2));
        });
    return ImmutableMap.copyOf(collector1);
  }
}
