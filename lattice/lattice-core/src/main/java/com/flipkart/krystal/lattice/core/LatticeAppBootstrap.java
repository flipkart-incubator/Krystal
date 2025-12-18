package com.flipkart.krystal.lattice.core;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.flipkart.krystal.lattice.core.doping.DopantSpec;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import lombok.Value;

@Value
public final class LatticeAppBootstrap {

  private final ImmutableMap<Class<? extends DopantSpec>, DopantSpec> configuredSpecs;

  public LatticeAppBootstrap(DopantSpec<?, ?, ?>... dopantSpecs) {
    configuredSpecs =
        Arrays.stream(dopantSpecs).collect(toImmutableMap(DopantSpec::getClass, identity()));
  }
}
