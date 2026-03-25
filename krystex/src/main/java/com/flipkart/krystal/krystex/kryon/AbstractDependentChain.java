package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract sealed class AbstractDependentChain implements DependentChain
    permits DefaultDependentChain, DependentChainStart {

  // Must use ConcurrentHashMap for thread safety - else we will encounter
  // ConcurrentModificationException
  @EqualsAndHashCode.Exclude @ToString.Exclude
  private final ConcurrentHashMap<DependentChainKey, DependentChain> dependenciesInternPool =
      new ConcurrentHashMap<>();

  @Override
  public DependentChain extend(VajramID vajramID, Dependency dependency) {
    DependentChainKey key = new DependentChainKey(vajramID.id(), dependency);
    DependentChain dependentChain = dependenciesInternPool.get(key);
    if (dependentChain == null) {
      DependentChain newDependentChain = new DefaultDependentChain(vajramID, dependency, this);
      DependentChain existing = dependenciesInternPool.putIfAbsent(key, newDependentChain);
      dependentChain = existing != null ? existing : newDependentChain;
    }
    return dependentChain;
  }

  /** Optimized key class for ConcurrentHashMap */
  private static class DependentChainKey {
    private final String vajramId;
    private final Dependency dependency;
    private final int hash;

    public DependentChainKey(String vajramId, Dependency dependency) {
      this.dependency = dependency;
      this.vajramId = vajramId;
      //  Manually inline the hash calculation to avoid Objects.hash array allocation
      this.hash = 31 * vajramId.hashCode() + (dependency == null ? 0 : dependency.hashCode());
    }

    @Override
    public int hashCode() {
      return hash; // Instant return
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) return true;
      if (!(o instanceof DependentChainKey that)) return false;
      return hash == that.hash
          && vajramId.equals(that.vajramId)
          && Objects.equals(dependency, that.dependency);
    }
  }
}
