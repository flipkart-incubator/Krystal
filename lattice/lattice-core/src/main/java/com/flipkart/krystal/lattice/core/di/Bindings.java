package com.flipkart.krystal.lattice.core.di;

import com.flipkart.krystal.lattice.core.di.BindingKey.AnnotationKey;
import com.flipkart.krystal.lattice.core.di.BindingKey.AnnotationTypeKey;
import com.flipkart.krystal.lattice.core.di.BindingKey.SimpleKey;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Bindings {
  private final Map<BindingKey<?>, Object> bindings = new LinkedHashMap<>();

  public <T> void bind(Class<T> key, T to) {
    bindings.put(new SimpleKey<>(key), to);
  }

  public <T> void bind(Class<T> key, Class<? extends Annotation> annotationType, T to) {
    bindings.put(new AnnotationTypeKey<>(key, annotationType), to);
  }

  public <T> void bind(Class<T> key, Annotation annotation, T to) {
    bindings.put(new AnnotationKey<>(key, annotation), to);
  }

  public ImmutableMap<BindingKey<?>, Object> asMap() {
    return ImmutableMap.copyOf(bindings);
  }
}
