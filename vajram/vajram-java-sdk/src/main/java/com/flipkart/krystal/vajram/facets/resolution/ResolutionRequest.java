package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.RequestBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public record ResolutionRequest(
    int dependencyId,
    ImmutableSet<Integer> inputsToResolve,
    ImmutableList<RequestBuilder<Object>> depRequests) {}
