package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;

public abstract sealed class AbstractDependentChain implements DependentChain
    permits DefaultDependentChain, DependentChainStart {

  @EqualsAndHashCode.Exclude @ToString.Exclude
  private final Map<VajramID, ConcurrentHashMap<Dependency, DependentChain>>
      dependenciesInternPool = new ConcurrentHashMap<>();

  @Override
  public DependentChain extend(VajramID vajramID, Dependency dependency) {
    return dependenciesInternPool
        .computeIfAbsent(vajramID, _n -> new ConcurrentHashMap<>())
        .computeIfAbsent(dependency, dep -> new DefaultDependentChain(vajramID, dep, this));
  }
}
