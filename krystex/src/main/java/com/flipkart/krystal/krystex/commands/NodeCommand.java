package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;

public sealed interface NodeCommand permits BatchCommand, GranularCommand, Flush {
  NodeId nodeId();

  DependantChain dependantChain();
}
