package com.flipkart.krystal.lattice.core.doping;

import java.lang.annotation.Annotation;

public interface DopantInitData<
    A extends Annotation,
    C extends DopantConfig,
    S extends DopantSpec<A, C, ? extends Dopant<A, C>>> {
  A annotation();

  C config();

  S spec();
}
