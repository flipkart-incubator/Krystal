package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;

import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import lombok.Builder;
import lombok.Singular;

public record KryonExecutorConfig(
    LogicDecorationOrdering logicDecorationOrdering,
    Map<String, List<OutputLogicDecoratorConfig>> requestScopedLogicDecoratorConfigs,
    ImmutableSet<DependantChain> disabledDependantChains,
    KryonExecStrategy kryonExecStrategy,
    GraphTraversalStrategy graphTraversalStrategy,
    @Singular Map<String, KryonDecoratorConfig> requestScopedKryonDecoratorConfigs,
    Optional<ExecutorService> customExecutorService,
    boolean debug) {

  @Builder(toBuilder = true)
  public KryonExecutorConfig {
    if (kryonExecStrategy == null) {
      kryonExecStrategy = BATCH;
    }
    if (graphTraversalStrategy == null) {
      graphTraversalStrategy = DEPTH;
    }
    if (requestScopedKryonDecoratorConfigs == null) {
      requestScopedKryonDecoratorConfigs = Map.of();
    }
    if (customExecutorService == null) {
      customExecutorService = Optional.empty();
    }
    if (disabledDependantChains == null) {
      disabledDependantChains = ImmutableSet.of();
    }
    if (logicDecorationOrdering == null) {
      logicDecorationOrdering = LogicDecorationOrdering.none();
    }
    if (requestScopedLogicDecoratorConfigs == null) {
      requestScopedLogicDecoratorConfigs = ImmutableMap.of();
    }
  }
}
