package com.flipkart.krystal.caramel.model;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class ValueImpl<T, P extends WorkflowPayload> implements Value<T, P> {

  private final Field<T, P> field;
  private final P payload;

  @MonotonicNonNull private T value;

  public ValueImpl(Field<T, P> field, P payload) {
    this.field = field;
    this.payload = payload;
  }

  @Override
  public void set(T value) {
    if (this.value != null) {
      throw new ImmutabilityViolationException(this);
    }
    this.value = value;
  }

  @Override
  public Optional<T> get() {
    return Optional.ofNullable(value);
  }

  @Override
  public T getOrThrow() {
    return get().orElseThrow(() -> new AccessBeforeInitializationException(this));
  }

  @Override
  public Field<T, P> field() {
    return field;
  }

  @Override
  public P payload() {
    return payload;
  }
}
