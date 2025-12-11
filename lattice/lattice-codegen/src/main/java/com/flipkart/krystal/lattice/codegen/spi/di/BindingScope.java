package com.flipkart.krystal.lattice.codegen.spi.di;

import static java.util.Objects.requireNonNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface BindingScope {

  enum StandardBindingScope implements BindingScope {
    UNKNOWN_SCOPE,
    NO_SCOPE,
    REQUEST,
    LAZY_SINGLETON,
    EAGER_SINGLETON;

    public static BindingScope of(@Nullable AnnotationMirror annotation) {
      if (annotation == null) {
        return NO_SCOPE;
      }
      if (annotation.getAnnotationType().asElement() instanceof TypeElement typeElement
          && typeElement
              .getQualifiedName()
              .contentEquals(requireNonNull(RequestScoped.class.getCanonicalName()))) {
        return REQUEST;
      }
      if (annotation.getAnnotationType().asElement() instanceof TypeElement typeElement
          && typeElement
              .getQualifiedName()
              .contentEquals(requireNonNull(ApplicationScoped.class.getCanonicalName()))) {
        return LAZY_SINGLETON;
      }
      return UNKNOWN_SCOPE;
    }
  }
}
