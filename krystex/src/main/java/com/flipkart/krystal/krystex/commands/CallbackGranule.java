package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.request.RequestId;

public record CallbackGranule(
    NodeId nodeId,
    String dependencyName,
    Results<Object> results,
    RequestId requestId,
    DependantChain dependantChain)
    implements GranularCommand {}
