package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.traits.StaticDispatchPolicy;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Builder;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Configurations for a particular execution within a KryonExecutor.
 *
 * @param executionId A unique execution id
 * @param disabledDependentChains if any of these dependant chains are encountered during the
 *     execution, that execution path is terminated
 * @param staticDispatchQualifier if the vajram being executed is a trait and static dispatch is
 *     configured for that vajram, this qualifier is used to determine the bound vajram Id
 */
@Builder(toBuilder = true)
public record KryonExecutionConfig(
    String executionId,
    ImmutableSet<DependentChain> disabledDependentChains,
    @Nullable Annotation staticDispatchQualifier) {

  private static final AtomicLong EXEC_COUNT = new AtomicLong();

  public KryonExecutionConfig {
    if (executionId == null) {
      executionId = "KryonExecution-" + EXEC_COUNT.getAndIncrement();
    }
    if (disabledDependentChains == null) {
      disabledDependentChains = ImmutableSet.of();
    }
    if (staticDispatchQualifier != null) {
      StaticDispatchPolicy.isValidQualifier(staticDispatchQualifier);
    }
  }
}
