package com.flipkart.krystal.caramel.model;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class SimpleCaramelField<T, P extends WorkflowPayload> implements CaramelField<T, P> {
  private final String name;
  private final Class<P> payloadType;
  private final Function<P, T> getter;
  private final BiConsumer<P, T> setter;

  public SimpleCaramelField(
      String name, Class<P> payloadType, Function<P, T> getter, BiConsumer<P, T> setter) {
    this.name = name;
    this.payloadType = payloadType;
    this.getter = getter;
    this.setter = setter;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Class<P> getPayloadType() {
    return payloadType;
  }

  @Override
  public T getValue(P payload) {
    return getter.apply(payload);
  }

  @Override
  public void setValue(P payload, T value) {
    setter.accept(payload, value);
  }
}
