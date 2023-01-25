package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.node.NodeId;
import com.google.common.collect.ImmutableList;

public sealed interface NodeCommand
    permits ExecuteWithAllInputs, ExecuteWithDependency, ExecuteWithInputs, SkipNode, Terminate {

  NodeId nodeId();

  RequestId requestId();

  default boolean shouldTerminate() {
    return false;
  }

  default ImmutableList<NodeId> dependants() {
    return ImmutableList.of();
  }
}
