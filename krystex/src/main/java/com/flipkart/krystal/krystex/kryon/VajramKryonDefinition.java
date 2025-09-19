package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.GraphExecutionData;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.GraphExecutionLogic;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.krystex.resolution.FacetsFromRequest;
import com.flipkart.krystal.krystex.resolution.Resolver;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;

/**
 * A stateless, reusable definition of a Kryon
 *
 * @param dependencyKryons Map of dependency name to kryonId.
 */
public record VajramKryonDefinition(
    VajramID vajramID,
    ImmutableSet<Facet> facets,
    KryonLogicId outputLogicId,
    ImmutableMap<ResolverDefinition, Resolver> resolversByDefinition,
    LogicDefinition<CreateNewRequest> createNewRequest,
    LogicDefinition<FacetsFromRequest> facetsFromRequest,
    KryonDefinitionRegistry kryonDefinitionRegistry,
    GraphExecutionLogic graphExecutionLogic,
    KryonDefinitionView view,
    ElementTags tags)
    implements KryonDefinition {

  public VajramKryonDefinition(
      VajramID vajramID,
      Set<? extends Facet> facets,
      KryonLogicId outputLogicId,
      ImmutableMap<ResolverDefinition, Resolver> resolversByDefinition,
      LogicDefinition<CreateNewRequest> createNewRequest,
      LogicDefinition<FacetsFromRequest> facetsFromRequest,
      KryonDefinitionRegistry kryonDefinitionRegistry,
      GraphExecutionLogic graphExecutionLogic,
      ElementTags tags) {
    this(
        vajramID,
        ImmutableSet.copyOf(facets),
        outputLogicId,
        resolversByDefinition,
        createNewRequest,
        facetsFromRequest,
        kryonDefinitionRegistry,
        graphExecutionLogic,
        KryonDefinitionView.createView(facets, resolversByDefinition),
        tags);
  }

  public ImmutableSet<Dependency> dependencies() {
    return view.dependencies();
  }

  public VajramKryonDefinition {
    view.resolverDefinitionsByDependencies()
        .forEach(
            (dep, resolvers) -> {
              if (resolvers.stream().filter(r -> r.definition().canFanout()).count() > 1) {
                throw new IllegalStateException(
                    "Multiple fanout resolvers found for dependency %s of vajram %s. This is not supported."
                        .formatted(dep, vajramID.id()));
              }
            });
  }

  public <T> OutputLogicDefinition<T> getOutputLogicDefinition() {
    return kryonDefinitionRegistry().logicDefinitionRegistry().getOutputLogic(outputLogicId());
  }

  public void executeGraph(GraphExecutionData graphExecutionData) {
    graphExecutionLogic.executeGraph(graphExecutionData);
  }

  @Override
  public ImmutableMap<Integer, Facet> facetsById() {
    return view.facetsById();
  }

  @Override
  public ImmutableSet<Facet> facetsByType(FacetType facetType) {
    return view.facetsByType().getOrDefault(facetType, ImmutableSet.of());
  }

  public ImmutableMap<Optional<Facet>, ImmutableSet<Resolver>> resolverDefinitionsByInput() {
    return view.resolverDefinitionsBySource();
  }

  public ImmutableSet<Dependency> dependenciesWithNoResolvers() {
    return view.dependenciesWithNoResolvers();
  }

  public ImmutableMap<Dependency, ImmutableSet<Resolver>> resolverDefinitionsByDependencies() {
    return view.resolverDefinitionsByDependencies();
  }

  public Set<Facet> givenFacets() {
    return view.givenFacets();
  }

  public ImmutableMap<Facet, ImmutableSet<Dependency>> dependenciesByBoundFacet() {
    return view.dependenciesByBoundFacet();
  }

  public ImmutableSet<Dependency> dependenciesWithNoFacetResolvers() {
    return view.dependenciesWithNoFacetResolvers();
  }

  public ImmutableMap<Dependency, ImmutableSet<Facet>> dependencyToBoundFacetsMapping() {
    return view.dependencyToBoundFacetsMapping();
  }
}
