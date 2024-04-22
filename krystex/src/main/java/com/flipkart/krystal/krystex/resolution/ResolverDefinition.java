package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableSet;

public record ResolverDefinition(
    int resolverId,
    KryonLogicId resolverKryonLogicId,
    ImmutableSet<Integer> boundFrom,
    Integer dependencyId,
    ImmutableSet<Integer> resolvedInputs) {}
