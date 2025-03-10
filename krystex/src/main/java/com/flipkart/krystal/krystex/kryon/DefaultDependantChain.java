package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DefaultDependantChain extends AbstractDependantChain {
  private final VajramID vajramID;
  @Getter private final Dependency latestDependency;
  @Getter private final DependantChain incomingDependantChain;
  private int _hashCodeCache;

  DefaultDependantChain(
      VajramID vajramID, Dependency latestDependency, DependantChain incomingDependantChain) {
    this.vajramID = vajramID;
    this.latestDependency = latestDependency;
    this.incomingDependantChain = incomingDependantChain;
  }

  public VajramID kryonId() {
    return vajramID;
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
    return "%s:%s[%s]".formatted(incomingDependantChain, vajramID.value(), latestDependency());
  }
}
