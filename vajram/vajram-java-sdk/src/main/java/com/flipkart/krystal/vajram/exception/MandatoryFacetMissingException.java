package com.flipkart.krystal.vajram.exception;

import com.flipkart.krystal.except.StackTracelessException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class MandatoryFacetMissingException extends StackTracelessException {

  private final String vajramName;
  private final String facetName;
  private @MonotonicNonNull String detailedMessage;

  public MandatoryFacetMissingException(String vajramId, String facetName) {
    this.vajramName = vajramId;
    this.facetName = facetName;
  }

  @Override
  public String getMessage() {
    if (detailedMessage == null) {
      detailedMessage = createMessage();
    }
    return detailedMessage;
  }

  private String createMessage() {
    return "Mandatory facet '%s' of vajram '%s' does not have a value"
        .formatted(facetName, vajramName);
  }
}
