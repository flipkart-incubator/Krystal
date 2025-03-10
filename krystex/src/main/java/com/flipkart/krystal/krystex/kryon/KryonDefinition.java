package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public sealed interface KryonDefinition permits VajramKryonDefinition, TraitKryonDefinition {

  ImmutableMap<Integer, Facet> facetsById();

  ImmutableSet<Facet> facetsByType(FacetType facetType);

  VajramID vajramID();

  ImmutableSet<Facet> facets();

  LogicDefinition<CreateNewRequest> createNewRequest();

  KryonDefinitionRegistry kryonDefinitionRegistry();

  ElementTags tags();
}
