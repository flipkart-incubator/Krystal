package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutor.DependencyExecStrategy;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutor.NodeExecStrategy;
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
    NodeExecStrategy nodeExecStrategy,
    GraphTraversalStrategy graphTraversalStrategy,
    DependencyExecStrategy dependencyExecStrategy) {

  public KrystalNodeExecutorConfig {
    if (nodeExecStrategy == null) {
      nodeExecStrategy = NodeExecStrategy.GRANULAR;
    }
    if (graphTraversalStrategy == null) {
      graphTraversalStrategy = GraphTraversalStrategy.DEPTH;
    }
    if (dependencyExecStrategy == null) {
      dependencyExecStrategy = DependencyExecStrategy.ONE_SHOT;
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
