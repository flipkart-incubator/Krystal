package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.facets.Dependency;

public sealed interface DependantChain permits AbstractDependantChain {

  DependantChain extend(KryonId kryonId, Dependency dependency);
}
