package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Facet;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DefaultDependantChain extends AbstractDependantChain {
  private final VajramID vajramID;
  private final Facet dependency;
  private final DependantChain dependantChain;
  private int _hashCodeCache;

  DefaultDependantChain(VajramID vajramID, Facet dependency, DependantChain dependantChain) {
    this.vajramID = vajramID;
    this.dependency = dependency;
    this.dependantChain = dependantChain;
  }

  public VajramID kryonId() {
    return vajramID;
  }

  public Facet dependency() {
    return dependency;
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
      this._hashCodeCache = System.identityHashCode(this);
    }
    return this._hashCodeCache;
  }

  @Override
  public String toString() {
    return "%s:%s[%s]".formatted(dependantChain, vajramID.value(), dependency());
  }
}
