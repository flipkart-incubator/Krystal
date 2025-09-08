package com.flipkart.krystal.lattice.core.doping;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import java.lang.annotation.Annotation;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class DopantSpecBuilderWithAnnotation<
        A extends Annotation,
        DS extends DopantSpec<A, NoConfiguration, ? extends DopantWithAnnotation<A>>>
    implements DopantSpecBuilder<A, NoConfiguration, DS> {

  public abstract DS _buildSpec(@Nullable A annotation);

  @Override
  public Class<NoConfiguration> _configurationType() {
    return NoConfiguration.class;
  }

  @Override
  public final DS _buildSpec(@Nullable A annotation, @Nullable NoConfiguration configuration) {
    return _buildSpec(annotation);
  }
}
