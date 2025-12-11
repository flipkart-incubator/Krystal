package com.flipkart.krystal.lattice.core.doping;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;

public interface SimpleDopantSpec<D extends SimpleDopant>
    extends DopantSpec<NoAnnotation, NoConfiguration, D> {
  Class<? extends D> dopantClass();

  @Override
  default Class<NoConfiguration> _configurationType() {
    return NoConfiguration.class;
  }
}
