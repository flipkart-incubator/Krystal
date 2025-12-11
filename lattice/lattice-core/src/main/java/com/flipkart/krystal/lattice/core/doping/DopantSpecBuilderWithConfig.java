package com.flipkart.krystal.lattice.core.doping;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class DopantSpecBuilderWithConfig<
        C extends DopantConfig,
        DS extends DopantSpec<NoAnnotation, C, ? extends DopantWithConfig<C>>>
    implements DopantSpecBuilder<NoAnnotation, C, DS> {

  public DS _buildSpec(@Nullable C configuration) {
    return _buildSpec();
  }

  @Override
  public Class<NoAnnotation> _annotationType() {
    return NoAnnotation.class;
  }
}
