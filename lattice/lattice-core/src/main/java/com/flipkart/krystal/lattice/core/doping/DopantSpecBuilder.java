package com.flipkart.krystal.lattice.core.doping;

import java.lang.annotation.Annotation;
import java.util.List;

public interface DopantSpecBuilder<
    A extends Annotation,
    C extends DopantConfig,
    DS extends DopantSpec<A, C, ? extends Dopant<A, C>>> {

  DS _buildSpec();

  Class<A> _annotationType();

  default List<DopantSpecBuilder<?, ?, ?>> getAdditionalDopants() {
    return List.of();
  }
}
