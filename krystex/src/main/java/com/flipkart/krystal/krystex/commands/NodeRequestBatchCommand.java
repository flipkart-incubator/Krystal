package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;

public record NodeRequestBatchCommand(
    NodeId nodeId, ImmutableMap<RequestId, NodeRequestCommand> subCommands)
    implements NodeCommand {}
