package com.flipkart.krystal.lattice.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flipkart.krystal.lattice.core.doping.DopantConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Function;
import lombok.experimental.NonFinal;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class LatticeAppConfig {
  private final ImmutableList<DopantConfig> dopants;

  @JsonIgnore @NonFinal @MonotonicNonNull
  private ImmutableMap<String, DopantConfig> dopantConfigsAsMap;

  @JsonCreator
  public LatticeAppConfig(@JsonProperty("dopants") ImmutableList<DopantConfig> dopants) {
    this.dopants = dopants;
  }

  public Map<String, DopantConfig> configsByDopantType() {
    if (dopantConfigsAsMap == null) {
      dopantConfigsAsMap =
          dopants.stream()
              .collect(ImmutableMap.toImmutableMap(DopantConfig::_dopantType, Function.identity()));
    }
    return dopantConfigsAsMap;
  }
}
