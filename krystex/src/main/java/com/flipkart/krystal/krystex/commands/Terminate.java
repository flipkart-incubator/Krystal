package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.DependantChainStart;
import com.flipkart.krystal.krystex.node.NodeId;

public record Terminate(NodeId nodeId, DependantChain nodeDependants) implements NodeCommand {

  public Terminate(NodeId nodeId) {
    this(nodeId, DependantChainStart.instance());
  }
}
