package com.flipkart.krystal.model;

import com.flipkart.krystal.except.StackTracelessException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class MandatoryDataMissingException extends StackTracelessException {

  private final String modelType;
  private final String dataFieldName;
  private @MonotonicNonNull String detailedMessage;

  public MandatoryDataMissingException(String modelType, String dataFieldName) {
    this.modelType = modelType;
    this.dataFieldName = dataFieldName;
  }

  @Override
  public String getMessage() {
    if (detailedMessage == null) {
      detailedMessage = createMessage();
    }
    return detailedMessage;
  }

  private String createMessage() {
    return "Mandatory data field '%s' of type '%s' does not have a value"
        .formatted(dataFieldName, modelType);
  }
}
