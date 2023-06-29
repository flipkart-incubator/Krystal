package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.NodeId;
import java.util.List;

public record ExecuteWithInputsBatch(NodeId nodeId, List<ExecuteWithInputs> subCommands)
    implements NodeRequestBatchCommand<ExecuteWithInputs> {}
