package com.flipkart.krystal.vajram.facets;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(FIELD)
@Retention(RUNTIME)
public @interface TraitDependency {
  final class Creator {
    public static @AutoAnnotation TraitDependency create() {
      return new AutoAnnotation_TraitDependency_Creator_create();
    }

    private Creator() {}
  }
}
