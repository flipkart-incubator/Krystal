package com.flipkart.krystal.lattice.core.doping;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class DopantSpecBuilderWithConfig<
        C extends DopantConfig, DS extends DopantSpec<NoAnnotation, C, DS>>
    implements DopantSpecBuilder<NoAnnotation, C, DS> {

  public abstract DS _buildSpec(@Nullable C configuration);

  @Override
  public Class<NoAnnotation> _annotationType() {
    return NoAnnotation.class;
  }

  @Deprecated
  @Override
  public final DS _buildSpec(@Nullable NoAnnotation annotation, @Nullable C configuration) {
    return _buildSpec(configuration);
  }
}
