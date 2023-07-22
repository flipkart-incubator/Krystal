package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.ResolverExecStrategy.MULTI;

import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.ResolverExecStrategy;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder(toBuilder = true)
public record KryonExecutorConfig(
    LogicDecorationOrdering logicDecorationOrdering,
    Map<String, List<MainLogicDecoratorConfig>> requestScopedLogicDecoratorConfigs,
    ImmutableSet<DependantChain> disabledDependantChains,
    ResolverExecStrategy resolverExecStrategy,
    KryonExecStrategy kryonExecStrategy,
    GraphTraversalStrategy graphTraversalStrategy,
    boolean debug) {

  public KryonExecutorConfig {
    if (kryonExecStrategy == null) {
      kryonExecStrategy = BATCH;
    }
    if (resolverExecStrategy == null || BATCH.equals(kryonExecStrategy)) {
      resolverExecStrategy = MULTI;
    }
    if (graphTraversalStrategy == null) {
      graphTraversalStrategy = DEPTH;
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
