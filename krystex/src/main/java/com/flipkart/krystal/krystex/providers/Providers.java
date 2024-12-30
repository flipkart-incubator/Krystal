package com.flipkart.krystal.krystex.providers;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public record Providers(Map<Class<?>, Provider<?>> providersMap) {

  public boolean containsProviderFor(Class<?> clazz) {
    return providersMap.containsKey(clazz) && clazz.isInstance(providersMap.get(clazz).get());
  }

  public Optional<Provider<?>> getProviderFor(Class<?> clazz) {
    if (!containsProviderFor(clazz)) {
      return Optional.empty();
    }
    return Optional.ofNullable(providersMap.get(clazz));
  }

  public static Providers empty() {
    return new Providers(Collections.emptyMap());
  }
}
