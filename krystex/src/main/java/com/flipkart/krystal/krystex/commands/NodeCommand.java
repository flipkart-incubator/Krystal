package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;

public sealed interface NodeCommand permits BatchNodeCommand, Flush, GranularNodeCommand{
  NodeId nodeId();

  DependantChain dependantChain();
}
