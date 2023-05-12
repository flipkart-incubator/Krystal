package com.flipkart.krystal.caramel.model;

import java.util.Optional;

public sealed interface Value<T, P extends WorkflowPayload> permits ValueImpl {

  CaramelField<T, P> field();

  P payload();

  Optional<T> get();

  T getOrThrow();

  void set(T value);
}
