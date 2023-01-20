package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.node.NodeId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public record ExecuteWithInputs(
    NodeId nodeId,
    ImmutableSet<String> inputNames,
    Inputs values,
    RequestId requestId,
    ImmutableList<NodeId> dependants)
    implements NodeCommand {}
