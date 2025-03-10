package com.flipkart.krystal.annos;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.flipkart.krystal.core.KrystalElement.Facet;
import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@HasCreator
@ApplicableToElements(Facet.class)
@Target({}) // Auto-created by the platform at runtime
@Retention(RUNTIME)
public @interface TraitDependency {
  final class Creator {
    public static @AutoAnnotation TraitDependency create() {
      return new AutoAnnotation_TraitDependency_Creator_create();
    }

    private Creator() {}
  }
}
