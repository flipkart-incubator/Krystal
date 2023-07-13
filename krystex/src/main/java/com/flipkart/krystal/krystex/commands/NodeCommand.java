package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.NodeId;

public sealed interface NodeCommand permits Flush, NodeDataCommand {
  NodeId nodeId();
}
