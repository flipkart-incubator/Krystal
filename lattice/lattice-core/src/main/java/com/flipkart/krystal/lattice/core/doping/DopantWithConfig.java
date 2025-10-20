package com.flipkart.krystal.lattice.core.doping;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;

public interface DopantWithConfig<C extends DopantConfig> extends Dopant<NoAnnotation, C> {}
