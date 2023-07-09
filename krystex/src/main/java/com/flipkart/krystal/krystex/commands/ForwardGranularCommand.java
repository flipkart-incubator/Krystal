package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableSet;

public record ForwardGranularCommand(
    NodeId nodeId,
    ImmutableSet<String> inputNames,
    Inputs values,
    DependantChain dependantChain,
    RequestId requestId)
    implements GranularNodeCommand {}
