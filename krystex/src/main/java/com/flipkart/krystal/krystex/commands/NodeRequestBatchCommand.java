package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.request.RequestId;
import java.util.Map;

public record NodeRequestBatchCommand(
    NodeId nodeId, Map<RequestId, NodeRequestCommand> subCommands)
    implements NodeCommand {}
