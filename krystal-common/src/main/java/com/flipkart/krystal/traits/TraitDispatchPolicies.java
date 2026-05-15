package com.flipkart.krystal.traits;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.flipkart.krystal.core.VajramID;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TraitDispatchPolicies {
  private final ImmutableMap<VajramID, TraitDispatchPolicy> traitDispatchPolicies;

  public TraitDispatchPolicies() {
    this(List.of());
  }

  public TraitDispatchPolicies(TraitDispatchPolicy... traitDispatchPolicies) {
    this(List.of(traitDispatchPolicies));
  }

  @Builder
  public TraitDispatchPolicies(List<TraitDispatchPolicy> traitDispatchPolicies) {
    this.traitDispatchPolicies =
        traitDispatchPolicies.stream()
            .collect(toImmutableMap(TraitDispatchPolicy::traitID, identity()));
  }

  public @Nullable TraitDispatchPolicy get(VajramID traitId) {
    return traitDispatchPolicies.get(traitId);
  }

  public List<TraitDispatchPolicy> traitDispatchPolicies() {
    return traitDispatchPolicies.values().asList();
  }

  public static class TraitDispatchPoliciesBuilder {

    public TraitDispatchPolicies.TraitDispatchPoliciesBuilder addTraitDispatchPolicies(
        List<? extends TraitDispatchPolicy> traitDispatchPolicies) {
      if (this.traitDispatchPolicies == null) {
        this.traitDispatchPolicies = new ArrayList<>();
      }
      this.traitDispatchPolicies.addAll(traitDispatchPolicies);
      return this;
    }
  }
}
