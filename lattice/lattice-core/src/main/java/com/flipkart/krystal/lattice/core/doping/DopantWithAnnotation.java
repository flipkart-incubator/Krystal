package com.flipkart.krystal.lattice.core.doping;

import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoAnnotation;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import java.lang.annotation.Annotation;

public interface DopantWithAnnotation<A extends Annotation> extends Dopant<A, NoConfiguration> {}
