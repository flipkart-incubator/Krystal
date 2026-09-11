package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.Nullable;

final class DependentChainSliceImpl extends AbstractDependentChainBase<DependentChainSlice>
    implements DependentChainSlice {

  DependentChainSliceImpl(
      VajramID vajramID,
      Dependency latestDependency,
      @Nullable DependentChainSlice incomingDependentChain) {
    super(vajramID, latestDependency, incomingDependentChain);
  }

  @Override
  protected DependentChainSlice _extend(VajramID vajramID, Dependency dependency) {
    return new DependentChainSliceImpl(vajramID, dependency, this);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    return obj instanceof DependentChainSliceImpl other
        && Arrays.equals(this.array(), other.array());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(this.array());
  }
}
