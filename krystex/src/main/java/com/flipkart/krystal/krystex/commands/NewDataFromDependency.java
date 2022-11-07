package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.Node;
import com.flipkart.krystal.krystex.SingleResult;
import com.google.common.collect.ImmutableCollection;

public record NewDataFromDependency(
    Node<?> node, String dependencyNodeId, ImmutableCollection<SingleResult<?>> newData)
    implements NodeCommand {}
