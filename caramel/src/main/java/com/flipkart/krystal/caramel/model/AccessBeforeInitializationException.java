package com.flipkart.krystal.caramel.model;

public class AccessBeforeInitializationException extends RuntimeException {

  public AccessBeforeInitializationException(Value<?, ? extends WorkflowPayload> fieldValue) {
    super(
        "Field %s of workflow payload %s cannot be accessed without initialization. Please check your workflow definition."
            .formatted(fieldValue.field().getName(), fieldValue.field().getPayloadType()));
  }
}
