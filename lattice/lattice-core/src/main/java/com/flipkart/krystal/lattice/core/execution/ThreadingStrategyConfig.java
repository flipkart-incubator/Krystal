package com.flipkart.krystal.lattice.core.execution;

import static com.flipkart.krystal.lattice.core.execution.ThreadingStrategyDopant.DOPANT_TYPE;

import com.flipkart.krystal.lattice.core.doping.DopantConfig;
import com.flipkart.krystal.lattice.core.doping.DopantType;

@DopantType(DOPANT_TYPE)
public record ThreadingStrategyConfig(int maxApplicationThreads) implements DopantConfig {

  @Override
  public String _dopantType() {
    return DOPANT_TYPE;
  }
}
