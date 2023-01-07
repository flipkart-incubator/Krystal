package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.RequestId;

public sealed interface NodeCommand permits ExecuteInputless, ExecuteWithInput, SkipNode {

  NodeId nodeId();

  RequestId requestId();
}
