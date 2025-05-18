package com.flipkart.krystal.lattice.core.dopants;

import java.lang.annotation.Annotation;

public interface DopantSpec<
    A extends Annotation, C extends DopantConfig, S extends DopantSpec<A, C, S>> {
  Class<? extends Dopant<?, ?, ?>> dopantClass();
}
