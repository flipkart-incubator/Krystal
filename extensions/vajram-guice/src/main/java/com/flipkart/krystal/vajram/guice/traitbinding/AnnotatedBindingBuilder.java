package com.flipkart.krystal.vajram.guice.traitbinding;

import com.flipkart.krystal.data.Request;
import java.lang.annotation.Annotation;

public class AnnotatedBindingBuilder<T extends Request<?>> extends LinkedBindingBuilder<T> {

  private final TraitBinder traitBinder;
  private final Class<T> traitDef;

  public AnnotatedBindingBuilder(TraitBinder traitBinder, Class<T> traitDef) {
    super(traitBinder, traitDef);
    this.traitBinder = traitBinder;
    this.traitDef = traitDef;
  }

  public LinkedBindingBuilder<T> annotatedWith(Annotation annotation) {
    return new LinkedBindingBuilder<>(traitBinder, traitDef, annotation);
  }

  public LinkedBindingBuilder<T> annotatedWith(Class<? extends Annotation> annotation) {
    return new LinkedBindingBuilder<>(traitBinder, traitDef, annotation);
  }
}
