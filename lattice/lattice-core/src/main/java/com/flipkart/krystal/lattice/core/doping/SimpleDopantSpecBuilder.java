package com.flipkart.krystal.lattice.core.dopants;

import com.flipkart.krystal.lattice.core.dopants.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.dopants.DopantConfig.NoConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class SimpleDopantSpecBuilder<
        DS extends DopantSpec<NoAnnotation, NoConfiguration, DS>>
    extends DopantSpecBuilder<NoAnnotation, NoConfiguration, DS> {

  public abstract DS build();

  @Override
  public Class<NoAnnotation> getAnnotationType() {
    return NoAnnotation.class;
  }

  @Override
  public Class<NoConfiguration> getConfigurationType() {
    return NoConfiguration.class;
  }

  @Override
  public final DS build(
      @Nullable NoAnnotation annotation, @Nullable NoConfiguration configuration) {
    throw new UnsupportedOperationException();
  }
}
