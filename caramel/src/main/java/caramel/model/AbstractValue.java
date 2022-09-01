package caramel.model;

import java.util.Optional;

public abstract class AbstractValue<T, P extends WorkflowPayload> implements Value<T, P> {
  private T value;

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

  public T getOrThrow() {
    return get().orElseThrow(() -> new AccessBeforeInitializationException(this));
  }
}
