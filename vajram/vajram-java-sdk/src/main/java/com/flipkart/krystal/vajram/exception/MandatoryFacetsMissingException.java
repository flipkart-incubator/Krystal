package com.flipkart.krystal.vajram.exception;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.except.StackTracelessException;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class MandatoryFacetsMissingException extends StackTracelessException {

  public static final String MANDATORY_FACET_MISSING = "mandatory facet missing";
  public static final String VAJRAM_S_DID_NOT_RECEIVE_THESE_MANDATORY_INPUTS_S =
      "Vajram %s did not receive these mandatory inputs: [ %s ]";
  private final VajramID vajramID;
  private final ImmutableMap<String, Throwable> failedMandatoryInputs;
  private @MonotonicNonNull String detailedMessage;

  public MandatoryFacetsMissingException(
      VajramID vajramID, Map<String, Throwable> failedMandatoryInputs) {
    this.vajramID = vajramID;
    this.failedMandatoryInputs = ImmutableMap.copyOf(failedMandatoryInputs);
  }

  @Override
  public String getMessage() {
    if (detailedMessage == null) {
      detailedMessage = createMessage();
    }
    return detailedMessage;
  }

  private String createMessage() {
    return VAJRAM_S_DID_NOT_RECEIVE_THESE_MANDATORY_INPUTS_S.formatted(
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
                                        .getOrDefault(
                                            s, new StackTracelessException(MANDATORY_FACET_MISSING))
                                        .getMessage())))
                .toList()));
  }
}
