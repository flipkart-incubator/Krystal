package com.flipkart.krystal.krystex.kryon;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import java.util.concurrent.ConcurrentHashMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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

  /** Key class for ConcurrentHashMap */
  // We want to cache the hashcode, but @EqualsAndHashCode doesn't support
  // records (see: https://github.com/projectlombok/lombok/issues/3246)
  @SuppressWarnings("ClassCanBeRecord")
  @EqualsAndHashCode(cacheStrategy = LAZY)
  private static class DependentChainKey {
    private final String vajramId;
    private final Dependency dependency;

    public DependentChainKey(String vajramId, Dependency dependency) {
      this.dependency = dependency;
      this.vajramId = vajramId;
    }
  }
}
