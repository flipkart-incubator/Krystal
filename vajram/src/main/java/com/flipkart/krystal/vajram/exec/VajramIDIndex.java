package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;

public final class VajramIDIndex implements AccessSpecIndex<VajramID> {
  private final Map<String, Vajram<?>> vajrams = new HashMap<>();

  @Override
  public AccessSpecMatchingResult<VajramID> getVajrams(VajramID vajramID) {
    Vajram<?> matchingVajram = vajrams.get(vajramID.vajramId());
    if (matchingVajram == null) {
      return new AccessSpecMatchingResult<>(
          ImmutableMap.of(), ImmutableMap.of(), ImmutableSet.of(vajramID));
    } else {
      return new AccessSpecMatchingResult<>(
          ImmutableMap.of(vajramID, matchingVajram), ImmutableMap.of(), ImmutableSet.of());
    }
  }

  @Override
  public void add(Vajram<?> vajram) {
    vajrams.put(vajram.getId(), vajram);
  }
}
