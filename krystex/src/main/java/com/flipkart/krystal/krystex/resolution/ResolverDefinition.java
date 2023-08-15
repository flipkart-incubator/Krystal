package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableSet;

public record ResolverDefinition(
    KryonLogicId resolverKryonLogicId,
    ImmutableSet<String> boundFrom,
    String dependencyName,
    ImmutableSet<String> resolvedInputNames) {}
