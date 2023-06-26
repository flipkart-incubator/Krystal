package com.flipkart.krystal.krystex.resolution;

import com.google.common.collect.ImmutableSet;

public record DependencyResolutionRequest(
    String dependencyName, ImmutableSet<String> inputsToResolve) {}
