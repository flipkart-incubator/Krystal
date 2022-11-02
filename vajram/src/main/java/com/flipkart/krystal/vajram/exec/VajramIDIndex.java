package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

public final class VajramIDIndex implements AccessSpecIndex<VajramID> {
  private final Map<String, Vajram<?>> vajrams = new HashMap<>();

  @Override
  public ImmutableMap<VajramID, Vajram<?>> getVajrams(VajramID vajramID) {
    Vajram<?> matchingVajram = vajrams.get(vajramID.vajramId());
    if (matchingVajram == null) {
      return ImmutableMap.of();
    }
    return ImmutableMap.of(vajramID, matchingVajram);
  }

  @Override
  public void add(Vajram<?> vajram) {
    vajrams.put(vajram.getId(), vajram);
  }
}
