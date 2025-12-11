package com.flipkart.krystal.traits;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.flipkart.krystal.core.VajramID;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TraitDispatchPolicies {
  private final ImmutableMap<VajramID, TraitDispatchPolicy> traitDispatchPolicies;

  public TraitDispatchPolicies() {
    this(List.of());
  }

  public TraitDispatchPolicies(@Nullable List<TraitDispatchPolicy> traitDispatchPolicies) {
    this.traitDispatchPolicies =
        traitDispatchPolicies == null
            ? ImmutableMap.of()
            : traitDispatchPolicies.stream()
                .collect(toImmutableMap(TraitDispatchPolicy::traitID, identity()));
  }

  public @Nullable TraitDispatchPolicy get(VajramID traitId) {
    return traitDispatchPolicies.get(traitId);
  }
}
