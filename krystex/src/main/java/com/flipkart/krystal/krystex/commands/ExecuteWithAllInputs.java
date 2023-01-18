package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.node.NodeId;
import com.google.common.collect.ImmutableList;

public record ExecuteWithAllInputs(
    NodeId nodeId, Inputs inputs, RequestId requestId, ImmutableList<NodeId> dependants)
    implements NodeCommand {}
