package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.DependentChainBase;
import com.google.common.collect.ImmutableSet;

public class SimpleDependentChainDisabler implements DependentChainDisabler {

  private final ImmutableSet<DependentChain> disabledDependentChains;
  private final ImmutableSet<DependentChainBase> disableDepChainsEndingWith;

  public SimpleDependentChainDisabler(ImmutableSet<DependentChain> disabledDependentChains) {
    this(disabledDependentChains, ImmutableSet.of());
  }

  /**
   * @param disabledDependentChains {@link DependentChain}s which are considered disabled - i.e. if
   *     we reach any of these dependent chains during dependent chain construction, the
   *     construction of subsequent dependent chains originating from that point is terminated - in
   *     other words, no further dependent chains are extended from this disable dependent chain.
   *     This is especially useful when there are loops in the graph, and you want to prevent the
   *     looped executions from going out of bounds. Graphs with loops must disable dependent chains
   *     beyond some threshold. Else, it will result in a {@link StackOverflowError}.
   * @param disableDepChainsEndingWith all dependent chains ending with any of these dependent
   *     chains are considered disabled. For example, let's say
   *     disabledDependentChainEndingWith=[V1:d1>V2:d2], then if we encounter any dependent chains
   *     like [Vx:dx>V1:d1>V2:d2] or [Vp:dp>Vq:dq>V1:d1>V2:d2], then construction of subsequent
   *     dependent chains is terminated - in other words, no further dependent chains are extended
   *     from such disable dependent chains.
   */
  public SimpleDependentChainDisabler(
      ImmutableSet<DependentChain> disabledDependentChains,
      ImmutableSet<DependentChainBase> disableDepChainsEndingWith) {
    this.disabledDependentChains = disabledDependentChains;
    this.disableDepChainsEndingWith = disableDepChainsEndingWith;
  }

  @Override
  public boolean isDisabled(DependentChain dependentChain) {
    if (disabledDependentChains.contains(dependentChain)) {
      return true;
    }
    for (DependentChainBase disabledSuffix : disableDepChainsEndingWith) {
      if (dependentChain.endsWith(disabledSuffix)) {
        return true;
      }
    }
    return false;
  }
}
