package com.flipkart.krystal.krystex.kryon;

import lombok.EqualsAndHashCode;

public final class DefaultDependantChain extends AbstractDependantChain {
  private final KryonId kryonId;
  private final String dependencyName;
  private final DependantChain dependantChain;

  DefaultDependantChain(KryonId kryonId, String dependencyName, DependantChain dependantChain) {
    this.kryonId = kryonId;
    this.dependencyName = dependencyName;
    this.dependantChain = dependantChain;
  }

  public KryonId kryonId() {
    return kryonId;
  }

  public String dependencyName() {
    return dependencyName;
  }

  public DependantChain dependantChain() {
    return dependantChain;
  }

  @Override
  public String toString() {
    return "%s:%s[%s]".formatted(dependantChain, kryonId.value(), dependencyName());
  }
}
