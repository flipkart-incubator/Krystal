package com.flipkart.krystal.lattice.core.di;

import com.flipkart.krystal.lattice.core.di.BindingKey.AnnotationKey;
import com.flipkart.krystal.lattice.core.di.BindingKey.AnnotationTypeKey;
import com.flipkart.krystal.lattice.core.di.BindingKey.SimpleKey;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Builder;
import org.checkerframework.checker.nullness.qual.NonNull;

@Builder
public final class Bindings {
  private final ImmutableMap<BindingKey<?>, Object> bindings;

  private Bindings(Map<BindingKey<?>, Object> bindings) {
    this.bindings = ImmutableMap.copyOf(bindings);
  }

  public ImmutableMap<BindingKey<?>, Object> asMap() {
    return ImmutableMap.copyOf(bindings);
  }

  public static class BindingsBuilder {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // used in the build method
    private final Map<BindingKey<?>, Object> bindings = new LinkedHashMap<>();

    public <T extends @NonNull Object> void bind(Class<T> key, T to) {
      bindings.put(new SimpleKey<>(key), to);
    }

    public <T extends @NonNull Object> void bind(
        Class<T> key, Class<? extends Annotation> annotationType, T to) {
      bindings.put(new AnnotationTypeKey<>(key, annotationType), to);
    }

    public <T extends @NonNull Object> void bind(Class<T> key, Annotation annotation, T to) {
      bindings.put(new AnnotationKey<>(key, annotation), to);
    }

    /**** Suppress setters which are not supposed to be used ****/

    @SuppressWarnings("unused")
    private BindingsBuilder bindings(Map<BindingKey<?>, Object> bindings) {
      throw new UnsupportedOperationException("Please use the bind methods instead");
    }
  }
}
