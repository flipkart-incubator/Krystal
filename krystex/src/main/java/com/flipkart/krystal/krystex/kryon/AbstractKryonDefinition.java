package com.flipkart.krystal.krystex.kryon;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

abstract sealed class AbstractKryonDefinition implements KryonDefinition
    permits TraitKryonDefinition, VajramKryonDefinition {
  private final ConcurrentMap<Class<?>, Object> customMetadata = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public <T> T getCustomMetadata(Class<T> clazz, Function<KryonDefinition, T> computer) {
    return (T) customMetadata.computeIfAbsent(clazz, _c -> computer.apply(this));
  }
}
