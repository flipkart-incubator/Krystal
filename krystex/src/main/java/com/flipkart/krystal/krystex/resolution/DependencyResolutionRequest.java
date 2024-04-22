package com.flipkart.krystal.krystex.resolution;

import java.util.List;

public record DependencyResolutionRequest(int dependencyId, List<Integer> resolverIds) {}
