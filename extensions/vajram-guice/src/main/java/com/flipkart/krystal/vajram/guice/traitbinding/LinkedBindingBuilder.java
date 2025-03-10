package com.flipkart.krystal.vajram.guice.traitbinding;

import static com.google.common.base.Preconditions.checkArgument;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.VajramRequestRoot;
import com.google.inject.Binder;
import com.google.inject.Key;
import java.lang.annotation.Annotation;

public class LinkedBindingBuilder<T extends Request<?>> {

  private TraitBinder traitBinder;
  private final Key<T> key;

  LinkedBindingBuilder(TraitBinder traitBinder, Class<T> traitDef, Annotation annotation) {
    this.traitBinder = traitBinder;
    this.key = Key.get(traitDef, annotation);
  }

  public LinkedBindingBuilder(
      TraitBinder traitBinder, Class<T> traitDef, Class<? extends Annotation> annotationType) {
    this.traitBinder = traitBinder;
    this.key = Key.get(traitDef, annotationType);
  }

  public LinkedBindingBuilder(TraitBinder traitBinder, Class<T> traitDef) {
    this.traitBinder = traitBinder;
    this.key = Key.get(traitDef);
  }

  public <C extends T> void to(Class<C> vajramReqType) {
    checkArgument(
        vajramReqType.getAnnotation(VajramRequestRoot.class) != null,
        "Only Concrete Vajram Request Interface can be bound");
    Binder guiceBinder = traitBinder.guiceBinder();
    if (guiceBinder != null) {
      guiceBinder.bind(key).to(vajramReqType);
    }
    traitBinder.addBinding(new TraitBinding(key, vajramReqType));
  }
}
