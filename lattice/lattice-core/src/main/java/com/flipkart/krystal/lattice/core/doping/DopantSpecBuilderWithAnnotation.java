package com.flipkart.krystal.lattice.core.dopants;

import com.flipkart.krystal.lattice.core.dopants.DopantConfig.NoConfiguration;
import java.lang.annotation.Annotation;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class DopantSpecBuilderWithAnnotation<
        A extends Annotation, DS extends DopantSpec<A, NoConfiguration, DS>>
    extends DopantSpecBuilder<A, NoConfiguration, DS> {

  public abstract DS build(@Nullable A annotation);

  @Override
  public Class<NoConfiguration> getConfigurationType() {
    return NoConfiguration.class;
  }

  @Override
  public final DS build(@Nullable A annotation, @Nullable NoConfiguration configuration) {
    throw new UnsupportedOperationException();
  }
}
