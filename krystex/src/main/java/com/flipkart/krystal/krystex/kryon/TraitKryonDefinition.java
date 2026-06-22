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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public record TraitKryonDefinition(
    VajramID vajramID,
    ImmutableSet<Facet> facets,
    LogicDefinition<CreateNewRequest> createNewRequest,
    KryonDefinitionRegistry kryonDefinitionRegistry,
    ElementTags allTags,
    KryonDefinitionView view,
    ConcurrentMap<Class<?>, Object> customMetadata)
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
        KryonDefinitionView.createView(facets, ImmutableMap.of()),
        new ConcurrentHashMap<>());
  }

  @Override
  public ImmutableSet<Facet> facetsByType(FacetType facetType) {
    return view.facetsByType().getOrDefault(facetType, ImmutableSet.of());
  }

  @SuppressWarnings("unchecked")
  public <T> T getCustomMetadata(Class<T> clazz, Function<KryonDefinition, T> computer) {
    return (T) customMetadata.computeIfAbsent(clazz, _c -> computer.apply(this));
  }
}
