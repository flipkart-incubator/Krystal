package com.flipkart.krystal.traits;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.flipkart.krystal.core.VajramID;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public class TraitDispatchPolicies {
  private final ImmutableMap<VajramID, TraitDispatchPolicy> traitDispatchPolicies;

  public TraitDispatchPolicies() {
    this(List.of());
  }

  public TraitDispatchPolicies(TraitDispatchPolicy... traitDispatchPolicies) {
    this(List.of(traitDispatchPolicies));
  }

  public TraitDispatchPolicies(Collection<? extends TraitDispatchPolicy> traitDispatchPolicies) {
    this(
        traitDispatchPolicies.stream()
            .collect(toImmutableMap(TraitDispatchPolicy::traitID, identity())));
  }

  private TraitDispatchPolicies(Map<VajramID, TraitDispatchPolicy> map) {
    traitDispatchPolicies = ImmutableMap.copyOf(map);
  }

  public @Nullable TraitDispatchPolicy get(VajramID traitId) {
    return traitDispatchPolicies.get(traitId);
  }

  public TraitDispatchPolicies merge(TraitDispatchPolicy... traitDispatchPolicies) {
    return merge(Arrays.asList(traitDispatchPolicies));
  }

  public TraitDispatchPolicies merge(TraitDispatchPolicies traitDispatchPolicies) {
    return merge(traitDispatchPolicies.traitDispatchPolicies.values());
  }

  public TraitDispatchPolicies merge(
      Collection<? extends TraitDispatchPolicy> traitDispatchPolicies) {
    Map<VajramID, TraitDispatchPolicy> map = new LinkedHashMap<>(this.traitDispatchPolicies);
    traitDispatchPolicies.forEach(
        traitDispatchPolicy -> {
          VajramID traitId = traitDispatchPolicy.traitID();
          if (map.put(traitId, traitDispatchPolicy) != null) {
            log.warn("Duplicate trait dispatch policy registered for trait {}", traitId);
          }
        });
    return new TraitDispatchPolicies(map);
  }
}
