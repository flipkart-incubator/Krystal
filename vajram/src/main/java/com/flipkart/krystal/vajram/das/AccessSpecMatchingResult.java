package com.flipkart.krystal.vajram.das;

import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;

public record AccessSpecMatchingResult<T extends DataAccessSpec>(
    ImmutableMap<T, VajramDefinition> exactMatches,
    ImmutableMap<T, VajramDefinition> superSetMatches,
    ImmutableCollection<T> unsuccessfulMatches) {

  public ImmutableMap<T, VajramDefinition> successfulMatches() {
    return ImmutableMap.<T, VajramDefinition>builder()
        .putAll(exactMatches)
        .putAll(superSetMatches)
        .build();
  }

  public boolean hasUnsuccessfulMatches() {
    return !unsuccessfulMatches().isEmpty();
  }

  /**
   * @return {@code true} if it is required to adapt the responses from vajrams contained in this
   *     object to avoid leaking unnecessary data to clients.
   * @see DataAccessSpec#adapt(Collection)
   */
  public boolean needsAdaptation() {
    return !superSetMatches().isEmpty();
  }
}
