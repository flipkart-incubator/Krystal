package com.flipkart.krystal.vajram.exception;

import static java.util.Collections.unmodifiableMap;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.except.KrystalCompletionException;
import com.flipkart.krystal.facets.Facet;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class MandatoryFacetsMissingException extends KrystalCompletionException {

  private final VajramID vajramID;
  private final Map<Facet, Throwable> failedMandatoryFacets;
  private @MonotonicNonNull String message;

  public MandatoryFacetsMissingException(
      VajramID vajramID, Map<Facet, Throwable> failedMandatoryFacets) {
    this.vajramID = vajramID;
    this.failedMandatoryFacets = unmodifiableMap(failedMandatoryFacets);
  }

  @Override
  public String getMessage() {
    if (message == null) {
      message = createMessage();
    }
    return message;
  }

  private String createMessage() {
    return "Vajram %s did not receive these mandatory facets: [ %s ]"
        .formatted(
            vajramID,
            String.join(
                ", ",
                failedMandatoryFacets.keySet().stream()
                    .map(
                        facet ->
                            "%s (Cause: %s)"
                                .formatted(
                                    facet,
                                    String.valueOf(
                                        failedMandatoryFacets
                                            .getOrDefault(facet, new RuntimeException())
                                            .getMessage())))
                    .toList()));
  }
}
