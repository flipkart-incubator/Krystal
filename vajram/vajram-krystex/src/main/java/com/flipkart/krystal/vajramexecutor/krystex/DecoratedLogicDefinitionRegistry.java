package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.node.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.node.IOLogicDefinition;
import com.flipkart.krystal.krystex.node.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.node.MainLogic;
import com.flipkart.krystal.krystex.node.NodeInputs;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.krystex.node.ResolverLogicDefinition;
import com.flipkart.krystal.krystex.node.ResolverNodeLogic;
import java.util.Set;
import java.util.function.Function;

public record DecoratedLogicDefinitionRegistry(LogicDefinitionRegistry delegate) {

  public <T> ResolverLogicDefinition newResolverLogic(
      String nodeId, Set<String> inputs, ResolverNodeLogic logic) {
    ResolverLogicDefinition def =
        new ResolverLogicDefinition(new NodeLogicId(nodeId), inputs, logic);
    delegate.addResolver(def);
    return def;
  }

  public <T> ComputeLogicDefinition<T> newComputeLogic(
      String nodeId, Set<String> inputs, Function<NodeInputs, T> logic) {
    ComputeLogicDefinition<T> def =
        new ComputeLogicDefinition<>(new NodeLogicId(nodeId), inputs, logic);
    delegate.addMainLogic(def);
    return def;
  }

  public <T> IOLogicDefinition<T> newIOLogic(
      NodeLogicId nodeLogicId, Set<String> inputs, MainLogic<T> nodeLogic) {
    IOLogicDefinition<T> def = new IOLogicDefinition<>(nodeLogicId, inputs, nodeLogic);
    delegate.addMainLogic(def);
    return def;
  }
}
