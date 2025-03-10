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
 * @param disabledDependantChains if any of these dependant chains are encountered during the
 *     execution, that execution path is terminated
 * @param staticDispatchQualifer if the vajram being executed is a trait and static dispatch is
 *     configured for that vajram, this qualifier is used to determine the bound vajram Id
 */
@Builder(toBuilder = true)
public record KryonExecutionConfig(
    String executionId,
    ImmutableSet<DependantChain> disabledDependantChains,
    @Nullable Annotation staticDispatchQualifer) {

  private static final AtomicLong EXEC_COUNT = new AtomicLong();

  public KryonExecutionConfig {
    if (executionId == null) {
      executionId = "KyonExecution-" + EXEC_COUNT.getAndIncrement();
    }
    if (disabledDependantChains == null) {
      disabledDependantChains = ImmutableSet.of();
    }
    if (staticDispatchQualifer != null) {
      StaticDispatchPolicy.isValidQualifier(staticDispatchQualifer);
    }
  }
}
