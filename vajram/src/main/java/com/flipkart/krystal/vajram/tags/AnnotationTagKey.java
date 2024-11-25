package com.flipkart.krystal.vajram.tags;

import java.lang.annotation.Annotation;

public record AnnotationTagKey(Object key, Class<? extends Annotation> annotationType) {
  public static AnnotationTagKey from(Class<? extends Annotation> annotationType) {
    return new AnnotationTagKey(annotationType, annotationType);
  }
}
