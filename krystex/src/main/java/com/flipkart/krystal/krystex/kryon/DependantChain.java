package com.flipkart.krystal.krystex.kryon;

public sealed interface DependantChain permits AbstractDependantChain {

  DependantChain extend(KryonId kryonId, String dependencyName);
}
