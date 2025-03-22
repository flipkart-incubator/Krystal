package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.kryon.DependentChain;

public record DependencyExecutionContext(
    VajramID vajramID,
    Dependency dependency,
    VajramID depVajramId,
    DependentChain dependentChain) {}
