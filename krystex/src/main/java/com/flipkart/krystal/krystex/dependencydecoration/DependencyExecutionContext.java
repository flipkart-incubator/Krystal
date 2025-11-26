package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;

public record DependencyExecutionContext(VajramID depVajramId, DependentChain dependentChain) {}
