package com.flipkart.krystal.lattice.core.doping;

import java.lang.annotation.Annotation;

public record AdditionalSpecBuilder<
    A extends Annotation,
    C extends DopantConfig,
    D extends Dopant<A, C>,
    DS extends DopantSpec<A, C, D>>(
    DopantSpecBuilder<A, C, DS> specBuilder) {}
