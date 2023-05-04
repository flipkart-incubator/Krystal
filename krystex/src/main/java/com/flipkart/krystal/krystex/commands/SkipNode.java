package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.ResolverCommand.SkipDependency;
import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;

public record SkipNode(NodeId nodeId, RequestId requestId, DependantChain dependencyChain,
                       SkipDependency skipDependencyCommand)
    implements NodeRequestCommand {

}
