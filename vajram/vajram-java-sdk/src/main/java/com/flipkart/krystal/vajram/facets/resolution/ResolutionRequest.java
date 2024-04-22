package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Request;
import com.google.common.collect.ImmutableSet;
import java.util.function.Supplier;

public record ResolutionRequest(
    int dependencyId, ImmutableSet<Integer> inputsToResolve, Request<Object> vajramRequest) {}
