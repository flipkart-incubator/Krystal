package com.flipkart.krystal.krystex;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public abstract sealed class OutputLogicDefinition<T> extends LogicDefinition<OutputLogic<T>>
    permits IOLogicDefinition, ComputeLogicDefinition {

  private final ImmutableSet<? extends Facet> usedComputedFacets;

  protected OutputLogicDefinition(
      KryonLogicId kryonLogicId,
      Set<? extends Facet> usedFacets,
      ElementTags tags,
      OutputLogic<T> outputLogic) {
    super(kryonLogicId, usedFacets, tags, outputLogic);
    this.usedComputedFacets =
        usedFacets.stream().filter(f -> !f.facetType().isGiven()).collect(toImmutableSet());
  }
}
