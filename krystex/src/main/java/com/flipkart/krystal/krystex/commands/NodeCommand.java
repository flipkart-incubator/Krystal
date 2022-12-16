package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.RequestId;

public sealed interface NodeCommand permits ExecuteWithInput, Execute {

  NodeId nodeId();

  RequestId requestId();
}
