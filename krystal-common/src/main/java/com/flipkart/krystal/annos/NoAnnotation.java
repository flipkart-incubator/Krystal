package com.flipkart.krystal.annos;

import com.google.auto.value.AutoAnnotation;
import lombok.experimental.UtilityClass;

/** A placeholder for use cases where no annotation is required. */
public @interface NoAnnotation {
  @UtilityClass
  final class Creator {
    private static final NoAnnotation INSTANCE = createNew();

    public static NoAnnotation create() {
      return INSTANCE;
    }

    private static @AutoAnnotation NoAnnotation createNew() {
      return new AutoAnnotation_NoAnnotation_Creator_createNew();
    }
  }
}
