package com.flipkart.krystal.vajram;

public final class Vajrams {

  public static String getVajramIdString(
      @SuppressWarnings("rawtypes") Class<? extends Vajram> aClass) {
    VajramDef annotation;
    Class<?> annotatedClass = aClass;
    do {
      annotation = annotatedClass.getAnnotation(VajramDef.class);
      if (annotation != null) {
        String vajramId = annotation.value();
        if (vajramId.isEmpty()) {
          vajramId = annotatedClass.getSimpleName();
        }
        return vajramId;
      }
      annotatedClass = annotatedClass.getSuperclass();
      if (annotatedClass == null) {
        break;
      }
    } while (Vajram.class.isAssignableFrom(annotatedClass));
    throw new IllegalStateException("Unable to find vajramId for class %s".formatted(aClass));
  }

  private Vajrams() {}
}
