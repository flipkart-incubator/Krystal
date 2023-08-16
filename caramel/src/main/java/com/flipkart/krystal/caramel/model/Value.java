package com.flipkart.krystal.caramel.model;

import java.util.Optional;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public sealed interface Value<T, P extends WorkflowPayload> permits ValueImpl {

  CaramelField<T, P> field();

  @UnknownInitialization
  P payload();

  Optional<T> get();

  T getOrThrow();

  void set(T value);
}
