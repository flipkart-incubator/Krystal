package com.flipkart.krystal.lattice.core.doping;

import java.lang.annotation.Annotation;

public interface DopantSpec<A extends Annotation, C extends DopantConfig, D extends Dopant<A, C>> {
  Class<? extends Dopant<A, C>> dopantClass();
}
