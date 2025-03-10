package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.kryon.DependantChain;

public record DependencyExecutionContext(
    VajramID vajramID,
    Dependency dependency,
    VajramID depVajramId,
    DependantChain dependantChain) {}
