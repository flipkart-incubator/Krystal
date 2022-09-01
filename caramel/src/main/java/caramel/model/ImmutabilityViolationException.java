package caramel.model;

public class ImmutabilityViolationException extends RuntimeException {
  public ImmutabilityViolationException(Value<?, ?> value) {
    super(
        "Cannot set the value twice for the field %s in object with root context %s"
            .formatted(value.field().getName(), value.getPayload().getClass().getName()));
  }
}
