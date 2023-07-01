package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.krystex.resolution.ResolverCommand.SkipDependency;

public record SkipNode(
    NodeId nodeId,
    RequestId requestId,
    DependantChain dependantChain,
    SkipDependency skipDependencyCommand)
    implements NodeInputCommand {}
