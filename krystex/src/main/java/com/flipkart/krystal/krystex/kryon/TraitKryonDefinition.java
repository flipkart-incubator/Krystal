package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public record TraitKryonDefinition(
    VajramID vajramID,
    ImmutableSet<Facet> facets,
    LogicDefinition<CreateNewRequest> createNewRequest,
    KryonDefinitionRegistry kryonDefinitionRegistry,
    ElementTags tags,
    KryonDefinitionView view)
    implements KryonDefinition {

  public TraitKryonDefinition(
      VajramID vajramID,
      Set<? extends Facet> facets,
      LogicDefinition<CreateNewRequest> createNewRequest,
      KryonDefinitionRegistry kryonDefinitionRegistry,
      ElementTags tags) {
    this(
        vajramID,
        ImmutableSet.copyOf(facets),
        createNewRequest,
        kryonDefinitionRegistry,
        tags,
        KryonDefinitionView.createView(facets, ImmutableMap.of()));
  }

  @Override
  public ImmutableMap<Integer, Facet> facetsById() {
    return view.facetsById();
  }

  @Override
  public ImmutableSet<Facet> facetsByType(FacetType facetType) {
    return view.facetsByType().getOrDefault(facetType, ImmutableSet.of());
  }
}
