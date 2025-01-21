package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.facets.Dependency;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;

public abstract sealed class AbstractDependantChain implements DependantChain
    permits DefaultDependantChain, DependantChainStart {

  @EqualsAndHashCode.Exclude @ToString.Exclude
  private final Map<KryonId, ConcurrentHashMap<Dependency, DependantChain>> dependenciesInternPool =
      new ConcurrentHashMap<>();

  @Override
  public DependantChain extend(KryonId kryonId, Dependency dependency) {
    return dependenciesInternPool
        .computeIfAbsent(kryonId, _n -> new ConcurrentHashMap<>())
        .computeIfAbsent(dependency, depName -> new DefaultDependantChain(kryonId, depName, this));
  }
}
