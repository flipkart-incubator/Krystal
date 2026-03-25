package com.flipkart.krystal.krystex.kryon;

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
  private final ConcurrentHashMap<String, ConcurrentHashMap<Dependency, DependentChain>>
      dependenciesInternPool = new ConcurrentHashMap<>();

  @Override
  public DependentChain extend(VajramID vajramID, Dependency dependency) {
    return dependenciesInternPool
        .computeIfAbsent(vajramID.id(), _n -> new ConcurrentHashMap<>())
        .computeIfAbsent(dependency, dep -> new DefaultDependentChain(vajramID, dep, this));
  }
}
