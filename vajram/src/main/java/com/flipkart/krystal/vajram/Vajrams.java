package com.flipkart.krystal.vajram;

import java.util.Optional;

public final class Vajrams {

  public static Optional<String> getVajramIdString(
      @SuppressWarnings("rawtypes") Class<? extends Vajram> aClass) {
    VajramDef annotation;
    Class<?> annotationClass = aClass;
    do {

      annotation = annotationClass.getAnnotation(VajramDef.class);
      annotationClass = annotationClass.getSuperclass();
      if (!Vajram.class.isAssignableFrom(annotationClass)) {
        annotationClass = null;
      }
    } while (annotation == null && annotationClass != null);
    return Optional.ofNullable(annotation).map(VajramDef::value);
  }

  private Vajrams() {}
}
