package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import org.checkerframework.checker.nullness.qual.Nullable;

// For performance reasons, equals and hashcode use Object class implementations. This is possible
// because AbstractDependentChainBase has an intern pool of these instances
final class DependentChainImpl extends AbstractDependentChainBase<DependentChain>
    implements DependentChain {

  private final DependentChain parent;

  DependentChainImpl(VajramID vajramID, Dependency latestDependency, DependentChain parent) {
    super(vajramID, latestDependency, parent);
    this.parent = parent;
  }

  @Override
  protected DependentChain _extend(VajramID vajramID, Dependency dependency) {
    return new DependentChainImpl(vajramID, dependency, this);
  }

  @Override
  public boolean startsWith(DependentChain dependentChain) {
    if (dependentChain instanceof DependentChainImpl other) {
      DependentChainNode[] array = this.array();
      DependentChainNode[] otherArray = other.array();
      if (otherArray.length > array.length) {
        return false;
      }
      for (int i = 0; i < otherArray.length; i++) {
        if (!array[i].equals(otherArray[i])) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public VajramID getFirstVajram() {
    return this.array()[0].vajramId();
  }

  @Override
  public @Nullable DependentChain parent() {
    return parent;
  }
}
