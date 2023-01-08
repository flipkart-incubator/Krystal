package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.node.NodeId;

public sealed interface NodeCommand permits ExecuteInputless, ExecuteWithInput, SkipNode {

  NodeId nodeId();

  RequestId requestId();
}
