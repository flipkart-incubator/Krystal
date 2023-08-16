package com.flipkart.krystal.caramel.model;

import java.util.Optional;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class ValueImpl<T, P extends WorkflowPayload> implements Value<T, P> {

  private final CaramelField<T, P> field;
  @NotOnlyInitialized private final P payload;

  @MonotonicNonNull private T value;

  public ValueImpl(CaramelField<T, P> field, @UnknownInitialization P payload) {
    this.field = field;
    this.payload = payload;
  }

  @Override
  public void set(T value) {
    if (this.value != null) {
      throw new ImmutabilityViolationException(this);
    }
    if (value != null) {
      this.value = value;
    }
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
  public CaramelField<T, P> field() {
    return field;
  }

  @Override
  public @UnknownInitialization P payload() {
    return payload;
  }
}
