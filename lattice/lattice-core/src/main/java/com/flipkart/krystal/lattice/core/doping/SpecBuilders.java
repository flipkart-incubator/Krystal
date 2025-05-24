package com.flipkart.krystal.lattice.core.doping;

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SpecBuilders {
  private ImmutableMap<Class<? extends DopantSpecBuilder>, DopantSpecBuilder> allSpecBuilders;

  @SuppressWarnings("unchecked")
  public <T extends DopantSpecBuilder<?, ?, ?>> T getSpecBuilder(Class<T> type) {
    return (T) allSpecBuilders.get(type);
  }
}
