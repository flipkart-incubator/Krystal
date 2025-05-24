package com.flipkart.krystal.lattice.core.doping;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class SimpleDopantSpecBuilder<
        DS extends DopantSpec<NoAnnotation, NoConfiguration, DS>>
    implements DopantSpecBuilder<NoAnnotation, NoConfiguration, DS> {

  public abstract DS _buildSpec();

  @Override
  public Class<NoAnnotation> _annotationType() {
    return NoAnnotation.class;
  }

  @Override
  public Class<NoConfiguration> _configurationType() {
    return NoConfiguration.class;
  }

  @Override
  public final DS _buildSpec(
      @Nullable NoAnnotation annotation, @Nullable NoConfiguration configuration) {
    return _buildSpec();
  }
}
