package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;

public record AccessSpecMatchingResult<T extends DataAccessSpec>(
    ImmutableMap<T, Vajram<?>> exactMatches,
    ImmutableMap<T, Vajram<?>> superSetMatches,
    ImmutableCollection<T> unsuccessfulMatches) {

  public ImmutableMap<T, Vajram<?>> successfulMatches() {
    return ImmutableMap.<T, Vajram<?>>builder()
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
  public boolean needsAdaption() {
    return !superSetMatches().isEmpty();
  }
}
