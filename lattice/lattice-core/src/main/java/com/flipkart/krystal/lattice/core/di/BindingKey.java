package com.flipkart.krystal.lattice.core.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public sealed interface BindingKey<T> {
  record SimpleKey<T>(Type type) implements BindingKey<T> {}

  record AnnotationTypeKey<T>(Type type, Class<? extends Annotation> annotationClass)
      implements BindingKey<T> {}

  record AnnotationKey<T>(Type type, Annotation annotation) implements BindingKey<T> {}
}
