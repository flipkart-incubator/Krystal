package com.flipkart.krystal.vajram;


public final class Vajrams {

  public static String getVajramIdString(
      @SuppressWarnings("rawtypes") Class<? extends Vajram> aClass) {
    VajramDef annotation;
    Class<?> annotationClass = aClass;
    do {
      annotation = annotationClass.getAnnotation(VajramDef.class);
      if (annotation != null) {
        String vajramId = annotation.value();
        if (vajramId.isEmpty()) {
          vajramId = annotationClass.getSimpleName();
        }
        return vajramId;
      }
      annotationClass = annotationClass.getSuperclass();
      if (annotationClass == null) {
        break;
      }
    } while (Vajram.class.isAssignableFrom(annotationClass));
    throw new IllegalStateException("Unable to find vajramId for class %s".formatted(aClass));
  }

  private Vajrams() {}
}
