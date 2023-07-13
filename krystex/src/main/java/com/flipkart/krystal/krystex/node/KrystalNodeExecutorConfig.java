package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutor.NodeExecStrategy;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutor.ResolverExecStrategy;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder(toBuilder = true)
public record KrystalNodeExecutorConfig(
    LogicDecorationOrdering logicDecorationOrdering,
    Map<String, List<MainLogicDecoratorConfig>> requestScopedLogicDecoratorConfigs,
    ImmutableSet<DependantChain> disabledDependantChains,
    ResolverExecStrategy resolverExecStrategy,
    NodeExecStrategy nodeExecStrategy) {

  public KrystalNodeExecutorConfig {
    if (resolverExecStrategy == null) {
      resolverExecStrategy = ResolverExecStrategy.MULTI;
    }
    if (nodeExecStrategy == null) {
      nodeExecStrategy = NodeExecStrategy.GRANULAR;
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
