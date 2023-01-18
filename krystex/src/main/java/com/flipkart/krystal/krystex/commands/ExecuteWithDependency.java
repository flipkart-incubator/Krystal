package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.node.NodeId;

public record ExecuteWithDependency(
    NodeId nodeId,
    String dependencyName,
    Results<Object> results,
    RequestId requestId)
    implements NodeCommand {}
