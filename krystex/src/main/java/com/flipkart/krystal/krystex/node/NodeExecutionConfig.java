package com.flipkart.krystal.krystex.node;

import com.google.common.collect.ImmutableSet;
import lombok.Builder;

@Builder(toBuilder = true)
public record NodeExecutionConfig(
    String executionId, ImmutableSet<DependantChain> disabledDependantChains) {

  @Override
  public ImmutableSet<DependantChain> disabledDependantChains() {
    return disabledDependantChains != null ? disabledDependantChains : ImmutableSet.of();
  }
}
