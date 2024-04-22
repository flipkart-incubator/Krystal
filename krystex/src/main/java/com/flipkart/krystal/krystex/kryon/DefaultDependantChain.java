package com.flipkart.krystal.krystex.kryon;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class DefaultDependantChain extends AbstractDependantChain {
  private final KryonId kryonId;
  private final int dependencyId;
  private final DependantChain dependantChain;
  private int _hashCodeCache;

  DefaultDependantChain(KryonId kryonId, int dependencyId, DependantChain dependantChain) {
    this.kryonId = kryonId;
    this.dependencyId = dependencyId;
    this.dependantChain = dependantChain;
  }

  public KryonId kryonId() {
    return kryonId;
  }

  public int dependencyId() {
    return dependencyId;
  }

  public DependantChain dependantChain() {
    return dependantChain;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    if (this._hashCodeCache == 0) {
      this._hashCodeCache = super.hashCode();
    }
    return this._hashCodeCache;
  }

  @Override
  public String toString() {
    return "%s:%s[%s]".formatted(dependantChain, kryonId.value(), dependencyId());
  }
}
