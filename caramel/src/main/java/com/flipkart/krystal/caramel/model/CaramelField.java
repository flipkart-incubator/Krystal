package com.flipkart.krystal.caramel.model;

public interface CaramelField<T, P extends WorkflowPayload> {
  String getName();

  Class<P> getPayloadType();

  T getValue(P payload);

  void setValue(P payload, T t);
}
