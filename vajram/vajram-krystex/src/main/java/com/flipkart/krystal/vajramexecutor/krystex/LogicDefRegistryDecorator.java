package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.ResolverLogic;
import com.flipkart.krystal.krystex.ResolverLogicDefinition;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.logic.LogicTag;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import java.util.function.Function;

public record LogicDefRegistryDecorator(LogicDefinitionRegistry delegate) {

  public <T> ResolverLogicDefinition newResolverLogic(
      String nodeId, Set<String> inputs, ResolverLogic logic) {
    ResolverLogicDefinition def =
        new ResolverLogicDefinition(new NodeLogicId(nodeId), inputs, logic, ImmutableMap.of());
    delegate.addResolver(def);
    return def;
  }

  public <T> ComputeLogicDefinition<T> newComputeLogic(
      String nodeId,
      Set<String> inputs,
      Function<Inputs, T> logic,
      ImmutableMap<String, LogicTag> logicTags) {
    ComputeLogicDefinition<T> def =
        new ComputeLogicDefinition<>(new NodeLogicId(nodeId), inputs, logic, logicTags);
    delegate.addMainLogic(def);
    return def;
  }

  public <T> IOLogicDefinition<T> newIOLogic(
      NodeLogicId nodeLogicId,
      Set<String> inputs,
      MainLogic<T> nodeLogic,
      ImmutableMap<String, LogicTag> logicTags) {
    IOLogicDefinition<T> def = new IOLogicDefinition<>(nodeLogicId, inputs, nodeLogic, logicTags);
    delegate.addMainLogic(def);
    return def;
  }
}
