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
import lombok.Getter;

public final class TraitKryonDefinition extends AbstractKryonDefinition {
  @Getter private final VajramID vajramID;
  @Getter private final ImmutableSet<Facet> facets;
  @Getter private final LogicDefinition<CreateNewRequest> createNewRequest;
  @Getter private final KryonDefinitionRegistry kryonDefinitionRegistry;
  @Getter private final ElementTags allTags;
  @Getter private final KryonDefinitionView view;

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

  private TraitKryonDefinition(
      VajramID vajramID,
      ImmutableSet<Facet> facets,
      LogicDefinition<CreateNewRequest> createNewRequest,
      KryonDefinitionRegistry kryonDefinitionRegistry,
      ElementTags allTags,
      KryonDefinitionView view) {
    this.vajramID = vajramID;
    this.facets = facets;
    this.createNewRequest = createNewRequest;
    this.kryonDefinitionRegistry = kryonDefinitionRegistry;
    this.allTags = allTags;
    this.view = view;
  }

  @Override
  public ImmutableSet<Facet> facetsByType(FacetType facetType) {
    return view.facetsByType().getOrDefault(facetType, ImmutableSet.of());
  }
}
