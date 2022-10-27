package com.flipkart.krystal.caramel.model;

import java.util.Optional;

public interface Value<T, P extends WorkflowPayload> {
  Field<T,P> field();

  P getPayload();

  void set(T value);

  Optional<T> get();

}
