package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.node.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.node.IOLogicDefinition;
import com.flipkart.krystal.krystex.node.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeInputs;
import com.flipkart.krystal.krystex.node.NodeLogic;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableList;
import java.util.Set;
import java.util.function.Function;

public record DecoratedLogicDefinitionRegistry(LogicDefinitionRegistry delegate) {

  public <T> ComputeLogicDefinition<T> newOneToManyComputeLogic(
      String nodeId, Set<String> inputs, Function<NodeInputs, ImmutableList<T>> logic) {
    ComputeLogicDefinition<T> def =
        new ComputeLogicDefinition<>(new NodeLogicId(nodeId), inputs, logic);
    delegate.add(def);
    return def;
  }

  public <T> IOLogicDefinition<T> newIOLogic(
      NodeLogicId nodeLogicId, Set<String> inputs, NodeLogic<T> nodeLogic) {
    IOLogicDefinition<T> def = new IOLogicDefinition<>(nodeLogicId, inputs, nodeLogic);
    delegate.add(def);
    return def;
  }
}
