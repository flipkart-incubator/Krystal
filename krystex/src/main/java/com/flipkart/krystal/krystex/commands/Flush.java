package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.DependantChainStart;
import com.flipkart.krystal.krystex.node.NodeId;

public record Flush(NodeId nodeId, DependantChain nodeDependants) implements NodeCommand {

  public Flush(NodeId nodeId) {
    this(nodeId, DependantChainStart.instance());
  }
}
