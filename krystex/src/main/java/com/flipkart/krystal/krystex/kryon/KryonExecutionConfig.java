package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.model.DependantChain;
import com.google.common.collect.ImmutableSet;
import lombok.Builder;

@Builder(toBuilder = true)
public record KryonExecutionConfig(
    String executionId, ImmutableSet<DependantChain> disabledDependantChains) {

  @Override
  public ImmutableSet<DependantChain> disabledDependantChains() {
    return disabledDependantChains != null ? disabledDependantChains : ImmutableSet.of();
  }
}
