package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.ResolverCommand.SkipDependency;
import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.request.RequestId;

public record SkipNode(
    NodeId nodeId,
    RequestId requestId,
    DependantChain dependantChain,
    SkipDependency skipDependencyCommand)
    implements NodeRequestCommand {}
