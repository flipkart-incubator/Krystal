package com.flipkart.krystal.krystex.kryon;

import com.google.common.collect.ImmutableSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import lombok.Builder;

@Builder(toBuilder = true)
public record KryonExecutionConfig(
    String executionId, ImmutableSet<DependantChain> disabledDependantChains) {

  private static final AtomicLong EXEC_COUNT = new AtomicLong();

  public KryonExecutionConfig {
    if (executionId == null) {
      executionId = "KyonExecution-" + EXEC_COUNT.getAndIncrement();
    }
  }

  @Override
  public ImmutableSet<DependantChain> disabledDependantChains() {
    return disabledDependantChains != null ? disabledDependantChains : ImmutableSet.of();
  }
}
