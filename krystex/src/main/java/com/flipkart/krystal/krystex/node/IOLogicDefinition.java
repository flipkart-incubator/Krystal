package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.node.NodeLogic;
import com.flipkart.krystal.krystex.node.NodeLogicDefinition;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import java.util.Set;

public final class IOLogicDefinition<T> extends NodeLogicDefinition<T> {

  private final NodeLogic<T> nodeLogic;

  public IOLogicDefinition(NodeLogicId nodeLogicId, Set<String> inputs, NodeLogic<T> nodeLogic) {
    super(nodeLogicId, inputs);
    this.nodeLogic = nodeLogic;
  }

  @Override
  public NodeLogic<T> logic() {
    return nodeLogic;
  }
}
