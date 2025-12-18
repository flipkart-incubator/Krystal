package com.flipkart.krystal.lattice.core.doping;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;

public abstract class SimpleDopantSpecBuilder<
        DS extends
            DopantSpec<
                    NoAnnotation, NoConfiguration, ? extends Dopant<NoAnnotation, NoConfiguration>>>
    implements DopantSpecBuilder<NoAnnotation, NoConfiguration, DS> {

  public abstract DS _buildSpec();

  @Override
  public Class<NoAnnotation> _annotationType() {
    return NoAnnotation.class;
  }
}
