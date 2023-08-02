package com.flipkart.krystal.krystex.kryon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;

public abstract sealed class AbstractDependantChain implements DependantChain
    permits DefaultDependantChain, DependantChainStart {

  @EqualsAndHashCode.Exclude @ToString.Exclude
  private final Map<KryonId, ConcurrentHashMap<String, DependantChain>> dependenciesInternPool =
      new ConcurrentHashMap<>();

  @Override
  public DependantChain extend(KryonId kryonId, String dependencyName) {
    return dependenciesInternPool
        .computeIfAbsent(kryonId, _n -> new ConcurrentHashMap<>())
        .computeIfAbsent(
            dependencyName, depName -> new DefaultDependantChain(kryonId, depName, this));
  }
}
