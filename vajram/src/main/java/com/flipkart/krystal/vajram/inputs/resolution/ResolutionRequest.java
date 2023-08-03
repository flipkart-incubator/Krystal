package com.flipkart.krystal.vajram.inputs.resolution;

import com.google.common.collect.ImmutableSet;

public record ResolutionRequest(String dependencyName, ImmutableSet<String> inputsToResolve) {}
