package com.flipkart.krystal.caramel.model;

import java.util.Optional;

public sealed interface Value<T, P extends WorkflowPayload> permits ValueImpl {

  Field<T, P> field();

  P payload();

  Optional<T> get();

  T getOrThrow();

  void set(T value);
}
