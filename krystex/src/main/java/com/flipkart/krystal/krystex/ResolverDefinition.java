package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableSet;

public record ResolverDefinition(
    NodeLogicId resolverNodeLogicId,
    ImmutableSet<String> boundFrom,
    String dependencyName,
    ImmutableSet<String> resolvedInputNames) {}
