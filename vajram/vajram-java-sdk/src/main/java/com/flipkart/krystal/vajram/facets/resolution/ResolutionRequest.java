package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Request;
import com.google.common.collect.ImmutableSet;

public record ResolutionRequest(
    int dependencyId, ImmutableSet<Integer> inputsToResolve, Request<Object> vajramRequest) {}
