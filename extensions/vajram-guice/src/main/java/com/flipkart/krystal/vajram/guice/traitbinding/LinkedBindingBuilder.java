package com.flipkart.krystal.vajram.guice.traitbinding;

import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramTraitDef;
import com.google.inject.Key;
import java.lang.annotation.Annotation;
import java.util.function.Supplier;

public class LinkedBindingBuilder {

  private TraitBinder traitBinder;
  private final Key<? extends VajramTraitDef<?>> key;

  LinkedBindingBuilder(
      TraitBinder traitBinder, Class<? extends VajramTraitDef<?>> traitDef, Annotation annotation) {
    this.traitBinder = traitBinder;
    this.key = Key.get(traitDef, annotation);
  }

  public LinkedBindingBuilder(
      TraitBinder traitBinder,
      Class<? extends VajramTraitDef<?>> traitDef,
      Class<? extends Annotation> annotationType) {
    this.traitBinder = traitBinder;
    this.key = Key.get(traitDef, annotationType);
  }

  public LinkedBindingBuilder(
      TraitBinder traitBinder, Class<? extends VajramTraitDef<?>> traitDef) {
    this.traitBinder = traitBinder;
    this.key = Key.get(traitDef);
  }

  public void to(Class<? extends VajramDef<?>> vajramDef) {
    to(() -> vajramDef);
  }

  public void to(Supplier<Class<? extends VajramDef<?>>> vajramDefSupplier) {
    traitBinder.addBinding(new TraitBinding(key, vajramDefSupplier));
  }
}
