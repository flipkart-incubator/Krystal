package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverLogicDefinition;
import java.util.HashMap;
import java.util.Map;

public final class LogicDefinitionRegistry {
  private final Map<NodeLogicId, MainLogicDefinition<?>> mainLogicDefinitions = new HashMap<>();
  private final Map<NodeLogicId, ResolverLogicDefinition> resolverLogicDefinitions =
      new HashMap<>();
  private final Map<NodeLogicId, MultiResolverDefinition> multiResolverDefinitions =
      new HashMap<>();

  public LogicDefinitionRegistry() {}

  public <T> MainLogicDefinition<T> getMain(NodeLogicId nodeLogicId) {
    //noinspection unchecked
    return (MainLogicDefinition<T>) mainLogicDefinitions.get(nodeLogicId);
  }

  public ResolverLogicDefinition getResolver(NodeLogicId nodeLogicId) {
    return resolverLogicDefinitions.get(nodeLogicId);
  }

  public MultiResolverDefinition getMultiResolver(NodeLogicId nodeLogicId) {
    return multiResolverDefinitions.get(nodeLogicId);
  }

  public void addMainLogic(MainLogicDefinition<?> mainLogicDefinition) {
    if (mainLogicDefinitions.containsKey(mainLogicDefinition.nodeLogicId())) {
      return;
    }
    mainLogicDefinitions.put(mainLogicDefinition.nodeLogicId(), mainLogicDefinition);
  }

  public void addResolver(ResolverLogicDefinition def) {
    if (resolverLogicDefinitions.containsKey(def.nodeLogicId())) {
      return;
    }
    resolverLogicDefinitions.put(def.nodeLogicId(), def);
  }

  public void addMultiResolver(MultiResolverDefinition def) {
    if (multiResolverDefinitions.containsKey(def.nodeLogicId())) {
      return;
    }
    multiResolverDefinitions.put(def.nodeLogicId(), def);
  }

  public void validate() {
    // TODO Check if all dependencies are present - there should be no dangling node ids
    // TODO Check that there are no loops in dependencies.
  }
}
