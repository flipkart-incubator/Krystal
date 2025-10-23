package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.kryon.DependentChain;

public record DependencyExecutionContext(Dependency dependency, DependentChain dependentChain) {}
