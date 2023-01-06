package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.node.NodeId;

public record SkipNode(NodeId nodeId, RequestId requestId, String reason) implements NodeCommand {}
