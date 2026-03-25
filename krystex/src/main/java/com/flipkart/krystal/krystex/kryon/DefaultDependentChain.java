package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Getter;

@EqualsAndHashCode(callSuper = false, cacheStrategy = CacheStrategy.LAZY)
public final class DefaultDependentChain extends AbstractDependentChain {
  @Getter private final VajramID vajramID;
  @Getter private final Dependency latestDependency;
  @Getter private final DependentChain incomingDependentChain;

  DefaultDependentChain(
      VajramID vajramID, Dependency latestDependency, DependentChain incomingDependentChain) {
    this.vajramID = vajramID;
    this.latestDependency = latestDependency;
    this.incomingDependentChain = incomingDependentChain;
  }

  @Override
  public String toString() {
    return "%s:%s[%s]".formatted(incomingDependentChain, vajramID.id(), latestDependency());
  }
}
