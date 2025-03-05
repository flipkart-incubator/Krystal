package com.flipkart.krystal.vajram.guice.traitbinding;

import com.flipkart.krystal.vajram.VajramTraitDef;
import java.lang.annotation.Annotation;

public class AnnotatedBindingBuilder extends LinkedBindingBuilder {

  private final TraitBinder traitBinder;
  private final Class<? extends VajramTraitDef<?>> traitDef;

  public AnnotatedBindingBuilder(
      TraitBinder traitBinder, Class<? extends VajramTraitDef<?>> traitDef) {
    super(traitBinder, traitDef);
    this.traitBinder = traitBinder;
    this.traitDef = traitDef;
  }

  public LinkedBindingBuilder annotatedWith(Annotation annotation) {
    return new LinkedBindingBuilder(traitBinder, traitDef, annotation);
  }

  public LinkedBindingBuilder annotatedWith(Class<? extends Annotation> annotation) {
    return new LinkedBindingBuilder(traitBinder, traitDef, annotation);
  }
}
