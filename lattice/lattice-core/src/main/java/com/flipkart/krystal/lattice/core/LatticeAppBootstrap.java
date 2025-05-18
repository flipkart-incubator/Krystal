package com.flipkart.krystal.lattice.core;

import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LatticeAppBootstrap {

  private final Map<Class<? extends DopantSpecBuilder>, DopantSpecBuilder> specBuilders =
      new LinkedHashMap<>();

  /**
   * Adds a {@link Dopant} to the lattice application. A "dopant" is used to add capabilities to the
   * lattice application or to control how the application logic is executed.
   *
   * @param specBuilder creates a spec builder for the dopant to add
   * @return this object
   */
  public LatticeAppBootstrap addDopant(DopantSpecBuilder specBuilder) {
    specBuilders.put(specBuilder.getClass(), specBuilder);
    return this;
  }

  public ImmutableMap<Class<? extends DopantSpecBuilder>, DopantSpecBuilder> specBuilders() {
    return ImmutableMap.copyOf(specBuilders);
  }
}
