package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;
import com.google.common.collect.ImmutableSet;

public record InitiateActiveDepChains(NodeId nodeId, ImmutableSet<DependantChain> dependantsChains)
    implements LogicDecoratorCommand {}
