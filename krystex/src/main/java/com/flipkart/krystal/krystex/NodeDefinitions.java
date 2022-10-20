package com.flipkart.krystal.krystex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class NodeDefinitions {
  private final Map<String, NodeDefinition<?>> nodeDefinitions = new HashMap<>();
  private final Map<String, Set<NodeDefinition<?>>> nodeDefinitionsByInputs = new HashMap<>();

  public void add(NodeDefinition<?> nodeDefinition) {
    nodeDefinitions.put(nodeDefinition.nodeId(), nodeDefinition);
    nodeDefinition
        .inputs()
        .forEach(
            input -> {
              nodeDefinitionsByInputs
                  .computeIfAbsent(input, s -> new HashSet<>())
                  .add(nodeDefinition);
              NodeDefinition<?> inputNodeDef = nodeDefinitions.get(input);
              if (inputNodeDef != null) {
                inputNodeDef.isAnInputTo(nodeDefinition.nodeId());
              }
            });
    nodeDefinitionsByInputs
        .get(nodeDefinition.nodeId())
        .forEach(dependent -> nodeDefinition.isAnInputTo(dependent.nodeId()));
  }

  public void validate() {
    // TODO Check if all dependencies are present - there should be no dangling node ids
    // TODO Check that there are no loops in dependencies.
  }
}
