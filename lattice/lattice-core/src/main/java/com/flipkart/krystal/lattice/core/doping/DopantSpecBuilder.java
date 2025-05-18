package com.flipkart.krystal.lattice.core.dopants;

import java.lang.annotation.Annotation;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class DopantSpecBuilder<
    A extends Annotation, C extends DopantConfig, DS extends DopantSpec<A, C, DS>> {

  public abstract DS build(@Nullable A annotation, @Nullable C configuration);

  public abstract Class<A> getAnnotationType();

  public abstract Class<C> getConfigurationType();

  public abstract String dopantType();

  public void configure(SpecBuilders allSpecBuilders) {}
}
