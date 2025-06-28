package com.flipkart.krystal.lattice.ext.guice.servlet;

import com.flipkart.krystal.lattice.core.di.BindingKey.AnnotationKey;
import com.flipkart.krystal.lattice.core.di.BindingKey.AnnotationTypeKey;
import com.flipkart.krystal.lattice.core.di.BindingKey.SimpleKey;
import com.flipkart.krystal.lattice.core.di.Bindings;
import com.flipkart.krystal.lattice.core.execution.ThreadingStrategy;
import com.flipkart.krystal.lattice.ext.guice.GuiceModuleBinder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.servlet.ServletModule;
import com.google.inject.servlet.ServletScopes;
import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuiceServletModuleBinder extends GuiceModuleBinder {
  public GuiceServletModuleBinder(Module... modules) {
    super(modules);
  }

  @Override
  protected List<Module> getExtensionModules() {
    return List.of(new ServletModule());
  }

  @Override
  public Closeable openRequestScope(Bindings seedMap, ThreadingStrategy threadingStrategy) {
    if (threadingStrategy != ThreadingStrategy.NATIVE_THREAD_PER_REQUEST) {
      throw new UnsupportedOperationException(
          threadingStrategy + " is not supported for request scoping");
    }

    Map<Key<?>, Object> map = new LinkedHashMap<>();
    seedMap
        .asMap()
        .forEach(
            (bindingKey, valueO) -> {
              Key<?> key;
              if (bindingKey instanceof SimpleKey<?> k) {
                key = Key.get(k.type());
              } else if (bindingKey instanceof AnnotationTypeKey<?> k) {
                key = Key.get(k.type(), k.annotationClass());
              } else if (bindingKey instanceof AnnotationKey<?> k) {
                key = Key.get(k.type(), k.annotation());
              } else {
                throw new UnsupportedOperationException(
                    bindingKey + " cannot be translated to Guice Key");
              }
              map.put(key, valueO);
            });

    return ServletScopes.scopeRequest(map).open();
  }
}
