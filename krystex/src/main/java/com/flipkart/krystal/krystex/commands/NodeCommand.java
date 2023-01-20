package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.node.NodeId;
import com.google.common.collect.ImmutableList;

public sealed interface NodeCommand
    permits ExecuteWithAllInputs, ExecuteWithDependency, ExecuteWithInputs, SkipNode {

  NodeId nodeId();

  RequestId requestId();

  default ImmutableList<NodeId> dependants() {
    return ImmutableList.of();
  }
}

