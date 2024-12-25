package com.flipkart.krystal.krystex.providers;

import java.util.Collections;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public record Providers(Map<Class<?>, Provider<?>> providersMap) {

  public boolean containsProviderFor(Class<?> clazz) {
    return providersMap.containsKey(clazz) && clazz.isInstance(providersMap.get(clazz).get());
  }

  public @Nullable Provider<?> getProviderFor(Class<?> clazz) {
    if (!containsProviderFor(clazz)) {
      throw new RuntimeException(
          "Provider for the class "
              + clazz.getSimpleName()
              + " was not found or the wrong provider is mapped against this class");
    }
    return providersMap.get(clazz);
  }

  public static Providers empty() {
    return new Providers(Collections.emptyMap());
  }
}
