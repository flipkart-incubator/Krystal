package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.ResolverLogic;
import com.flipkart.krystal.krystex.ResolverLogicDefinition;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.logic.LogicTag;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public record LogicDefRegistryDecorator(LogicDefinitionRegistry delegate) {

  public <T> ResolverLogicDefinition newResolverLogic(
      String nodeId, String nodeLogicId, Set<String> inputs, ResolverLogic logic) {
    ResolverLogicDefinition def =
        new ResolverLogicDefinition(
            new NodeLogicId(new NodeId(nodeId), nodeLogicId), inputs, logic, ImmutableMap.of());
    delegate.addResolver(def);
    return def;
  }

  public <T> MainLogicDefinition<T> newMainLogic(
      boolean isIOLogic,
      NodeLogicId nodeLogicId,
      Set<String> inputs,
      MainLogic<T> nodeLogic,
      ImmutableMap<String, LogicTag> logicTags) {
    MainLogicDefinition<T> def =
        isIOLogic
            ? new IOLogicDefinition<>(nodeLogicId, inputs, nodeLogic, logicTags)
            : new ComputeLogicDefinition<>(nodeLogicId, inputs, nodeLogic, logicTags);
    delegate.addMainLogic(def);
    return def;
  }
}
