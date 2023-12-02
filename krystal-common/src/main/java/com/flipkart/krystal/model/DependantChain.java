package com.flipkart.krystal.model;

public sealed interface DependantChain permits AbstractDependantChain {

  DependantChain extend(KryonId kryonId, String dependencyName);
}
