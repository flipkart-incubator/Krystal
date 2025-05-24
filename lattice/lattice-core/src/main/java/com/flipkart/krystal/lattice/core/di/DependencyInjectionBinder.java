package com.flipkart.krystal.lattice.core.di;

import com.flipkart.krystal.lattice.core.execution.ThreadingStrategy;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.google.auto.value.AutoAnnotation;
import jakarta.inject.Named;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DependencyInjectionBinder {
  <T> void bindToInstance(Class<T> type, T instance);

  <T> void bindInSingleton(Class<T> type);

  DependencyInjector getInjector();

  default @Nullable VajramInjectionProvider toVajramInjectionProvider() {
    return null;
  }

  Closeable openRequestScope(Map<BindingKey, Object> seedMap, ThreadingStrategy threadingStrategy);

  sealed interface BindingKey {
    record TypeKey(Type type) implements BindingKey {}

    record AnnotationClassType(Type type, Class<? extends Annotation> annotationClass)
        implements BindingKey {}

    record AnnotationType(Type type, Annotation annotation) implements BindingKey {}
  }

  @AutoAnnotation
  static Named named(String value) {
    return new AutoAnnotation_DependencyInjectionBinder_named(value);
  }
}
