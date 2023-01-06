package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.RequestId;

public record ExecuteInputless(NodeId nodeId, RequestId requestId) implements NodeCommand {}
