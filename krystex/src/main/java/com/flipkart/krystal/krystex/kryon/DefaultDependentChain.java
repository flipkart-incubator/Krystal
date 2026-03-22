package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DefaultDependentChain extends AbstractDependentChain {
  private final VajramID vajramID;
  @Getter private final Dependency latestDependency;
  @Getter private final DependentChain incomingDependentChain;
  private final int _hash;

  DefaultDependentChain(
      VajramID vajramID, Dependency latestDependency, DependentChain incomingDependentChain) {
    this.vajramID = vajramID;
    this.latestDependency = latestDependency;
    this.incomingDependentChain = incomingDependentChain;
    this._hash = System.identityHashCode(this);
  }

  public VajramID kryonId() {
    return vajramID;
  }

  @Override
  public int hashCode() {
    return this._hash;
  }

  @Override
  public String toString() {
    return "%s:%s[%s]".formatted(incomingDependentChain, vajramID.id(), latestDependency());
  }
}
