package com.flipkart.krystal.krystex;

import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.tags.ElementTags;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract sealed class OutputLogicDefinition<T> extends LogicDefinition<OutputLogic<T>>
    permits IOLogicDefinition, ComputeLogicDefinition {

  protected OutputLogicDefinition(
      KryonLogicId kryonLogicId,
      Set<? extends Facet> usedFacets,
      ElementTags tags,
      OutputLogic<T> outputLogic) {
    super(kryonLogicId, usedFacets, tags, outputLogic);
  }
}
