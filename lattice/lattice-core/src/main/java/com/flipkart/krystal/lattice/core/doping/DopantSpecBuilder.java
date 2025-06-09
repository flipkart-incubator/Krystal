package com.flipkart.krystal.lattice.core.doping;

import java.lang.annotation.Annotation;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DopantSpecBuilder<
    A extends Annotation,
    C extends DopantConfig,
    DS extends DopantSpec<A, C, ? extends Dopant<A, C>>> {

  DS _buildSpec(@Nullable A annotation, @Nullable C configuration);

  Class<A> _annotationType();

  Class<C> _configurationType();

  String _dopantType();

  default List<DopantSpecBuilder<?, ?, ?>> getAdditionalDopants() {
    return List.of();
  }

  default void _configure(SpecBuilders allSpecBuilders) {}
}
