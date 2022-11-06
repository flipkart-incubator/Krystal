package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableCollection;

public record NewDataFromDependency(
    Node<?> node, String dependencyNodeId, ImmutableCollection<SingleResult<?>> newData)
    implements NodeCommand {}
