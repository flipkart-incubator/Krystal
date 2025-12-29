package com.flipkart.krystal.model;

import com.flipkart.krystal.except.KrystalException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class MandatoryFieldMissingException extends KrystalException {

  private final String modelType;
  private final String dataFieldName;
  private @MonotonicNonNull String detailedMessage;

  public MandatoryFieldMissingException(String modelType, String dataFieldName) {
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
    return "Mandatory data field '%s' of the model '%s' does not have a value"
        .formatted(dataFieldName, modelType);
  }
}
