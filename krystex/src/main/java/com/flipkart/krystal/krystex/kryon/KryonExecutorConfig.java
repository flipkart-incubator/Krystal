package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;

import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.logicdecoration.MainLogicDecoratorConfig;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;

@Builder(toBuilder = true)
public record KryonExecutorConfig(
    LogicDecorationOrdering logicDecorationOrdering,
    Map<String, List<MainLogicDecoratorConfig>> requestScopedLogicDecoratorConfigs,
    ImmutableSet<DependantChain> disabledDependantChains,
    KryonExecStrategy kryonExecStrategy,
    GraphTraversalStrategy graphTraversalStrategy,
    Function<KryonId, List<KryonDecorator>> kryonDecoratorsProvider,
    boolean debug) {

  public KryonExecutorConfig {
    if (kryonExecStrategy == null) {
      kryonExecStrategy = BATCH;
    }
    if (graphTraversalStrategy == null) {
      graphTraversalStrategy = DEPTH;
    }
    if (kryonDecoratorsProvider == null) {
      kryonDecoratorsProvider = kryonId -> List.of();
    }
  }

  @Override
  public ImmutableSet<DependantChain> disabledDependantChains() {
    return disabledDependantChains != null ? disabledDependantChains : ImmutableSet.of();
  }

  @Override
  public LogicDecorationOrdering logicDecorationOrdering() {
    return logicDecorationOrdering != null
        ? logicDecorationOrdering
        : LogicDecorationOrdering.none();
  }

  @Override
  public Map<String, List<MainLogicDecoratorConfig>> requestScopedLogicDecoratorConfigs() {
    return requestScopedLogicDecoratorConfigs != null
        ? requestScopedLogicDecoratorConfigs
        : ImmutableMap.of();
  }
}
