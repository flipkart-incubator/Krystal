package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;

public record ExecuteWithInputs(
    NodeId nodeId,
    ImmutableSet<String> inputNames,
    Inputs values,
    DependantChain dependantChain,
    RequestId requestId)
    implements NodeRequestCommand {}
