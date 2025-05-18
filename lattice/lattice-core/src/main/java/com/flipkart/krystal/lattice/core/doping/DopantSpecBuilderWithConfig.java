package com.flipkart.krystal.lattice.core.dopants;

import com.flipkart.krystal.lattice.core.dopants.DopantConfig.NoAnnotation;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class DopantSpecBuilderWithConfig<
        C extends DopantConfig, DS extends DopantSpec<NoAnnotation, C, DS>>
    extends DopantSpecBuilder<NoAnnotation, C, DS> {

  public abstract DS build(@Nullable C configuration);

  @Override
  public Class<NoAnnotation> getAnnotationType() {
    return NoAnnotation.class;
  }

  @Deprecated
  @Override
  public final DS build(@Nullable NoAnnotation annotation, @Nullable C configuration) {
    throw new UnsupportedOperationException();
  }
}
