package com.flipkart.krystal.vajram.exception;

import static java.util.Collections.unmodifiableMap;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.except.StackTracelessException;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class MandatoryFacetsMissingException extends StackTracelessException {

  private final VajramID vajramID;
  private final Map<String, Throwable> failedMandatoryInputs;
  private @MonotonicNonNull String detailedMessage;

  public MandatoryFacetsMissingException(
      VajramID vajramID, Map<String, Throwable> failedMandatoryInputs) {
    this.vajramID = vajramID;
    this.failedMandatoryInputs = unmodifiableMap(failedMandatoryInputs);
  }

  @Override
  public String getMessage() {
    if (detailedMessage == null) {
      detailedMessage = createMessage();
    }
    return detailedMessage;
  }

  private String createMessage() {
    return "Vajram %s did not receive these mandatory inputs: [ %s ]"
        .formatted(
            vajramID,
            String.join(
                ", ",
                failedMandatoryInputs.keySet().stream()
                    .map(
                        s ->
                            "'%s' (Cause: %s)"
                                .formatted(
                                    s,
                                    String.valueOf(
                                        failedMandatoryInputs
                                            .getOrDefault(s, new RuntimeException())
                                            .getMessage())))
                    .toList()));
  }
}
