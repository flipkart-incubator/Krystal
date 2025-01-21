package com.flipkart.krystal.krystex.kryon;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.krystex.resolution.FacetsFromRequest;
import com.flipkart.krystal.krystex.resolution.Resolver;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * A stateless, reusable definition of a Kryon
 *
 * @param dependencyKryons Map of dependency name to kryonId.
 */
public record KryonDefinition(
    KryonId kryonId,
    ImmutableSet<Facet> facets,
    KryonLogicId outputLogicId,
    ImmutableMap<Dependency, KryonId> dependencyKryons,
    ImmutableMap<ResolverDefinition, Resolver> resolversByDefinition,
    Optional<KryonLogicId> multiResolverLogicId,
    LogicDefinition<CreateNewRequest> createNewRequest,
    LogicDefinition<FacetsFromRequest> facetsFromRequest,
    KryonDefinitionRegistry kryonDefinitionRegistry,
    KryonDefinitionView view,
    ElementTags tags) {

  public KryonDefinition(
      KryonId kryonId,
      Set<? extends Facet> facets,
      KryonLogicId outputLogicId,
      ImmutableMap<Dependency, KryonId> dependencyKryons,
      ImmutableMap<ResolverDefinition, Resolver> resolversByDefinition,
      Optional<KryonLogicId> multiResolverLogicId,
      LogicDefinition<CreateNewRequest> createNewRequest,
      LogicDefinition<FacetsFromRequest> facetsFromRequest,
      KryonDefinitionRegistry kryonDefinitionRegistry,
      ElementTags tags) {
    this(
        kryonId,
        ImmutableSet.copyOf(facets),
        outputLogicId,
        dependencyKryons,
        resolversByDefinition,
        multiResolverLogicId,
        createNewRequest,
        facetsFromRequest,
        kryonDefinitionRegistry,
        KryonDefinitionView.createView(facets, resolversByDefinition, dependencyKryons),
        tags);
  }

  public <T> OutputLogicDefinition<T> getOutputLogicDefinition() {
    return kryonDefinitionRegistry().logicDefinitionRegistry().getOutputLogic(outputLogicId());
  }

  public ImmutableMap<Integer, Facet> facetsById() {
    return view.facetsById();
  }

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

  /**
   * Useful data views over {@link KryonDefinition}'s data
   *
   * @param resolverDefinitionsBySource Maps each input with a set of resolver definitions that
   *     consume that input. {@link Optional#empty()} is mapped to those resolver definitions which
   *     do not take any inputs.
   * @param resolverDefinitionsByDependencies Maps each dependency with a set of resolverDefinitions
   *     which resolve inputs of that dependency
   * @param dependenciesWithNoResolvers Set of dependency names which have no resolvers.
   */
  private record KryonDefinitionView(
      ImmutableMap<Integer, Facet> facetsById,
      ImmutableMap<FacetType, ImmutableSet<Facet>> facetsByType,
      ImmutableMap<Optional<Facet>, ImmutableSet<Resolver>> resolverDefinitionsBySource,
      ImmutableMap<Dependency, ImmutableSet<Resolver>> resolverDefinitionsByDependencies,
      ImmutableSet<Dependency> dependenciesWithNoResolvers) {
    private static KryonDefinitionView createView(
        Set<? extends Facet> allFacets,
        ImmutableMap<ResolverDefinition, Resolver> resolversByDefinition,
        ImmutableMap<Dependency, KryonId> dependencyKryons) {
      ImmutableSet<Dependency> dependencyFacets = dependencyKryons.keySet();
      ImmutableMap<Dependency, ImmutableSet<Resolver>> resolverDefinitionsByDependencies =
          ImmutableMap.copyOf(
              resolversByDefinition.values().stream()
                  .collect(
                      groupingBy(d -> d.definition().target().dependency(), toImmutableSet())));
      ImmutableSet<Dependency> dependenciesWithNoResolvers =
          dependencyFacets.stream()
              .filter(
                  depName ->
                      resolverDefinitionsByDependencies
                          .getOrDefault(depName, ImmutableSet.of())
                          .isEmpty())
              .collect(toImmutableSet());
      Map<FacetType, Set<Facet>> facetsByType = new LinkedHashMap<>();
      for (Facet facet : allFacets) {
        for (FacetType facetType : facet.facetTypes()) {
          facetsByType.computeIfAbsent(facetType, _t -> new LinkedHashSet<>()).add(facet);
        }
      }
      return new KryonDefinitionView(
          allFacets.stream().collect(toImmutableMap(Facet::id, Function.identity())),
          facetsByType.entrySet().stream()
              .collect(toImmutableMap(Entry::getKey, e -> ImmutableSet.copyOf(e.getValue()))),
          createResolverDefinitionsBySource(resolversByDefinition),
          resolverDefinitionsByDependencies,
          dependenciesWithNoResolvers);
    }

    private static ImmutableMap<Optional<Facet>, ImmutableSet<Resolver>>
        createResolverDefinitionsBySource(
            ImmutableMap<ResolverDefinition, Resolver> resolversByDefinition) {
      Map<Optional<Facet>, ImmutableSet.Builder<Resolver>> resolverDefinitionsByInput =
          new LinkedHashMap<>();
      resolversByDefinition
          .values()
          .forEach(
              resolver -> {
                ImmutableSet<? extends Facet> sources = resolver.definition().sources();
                if (!sources.isEmpty()) {
                  sources.forEach(
                      facet ->
                          resolverDefinitionsByInput
                              .computeIfAbsent(Optional.of(facet), s -> ImmutableSet.builder())
                              .add(resolver));
                } else {
                  resolverDefinitionsByInput
                      .computeIfAbsent(Optional.empty(), s -> ImmutableSet.builder())
                      .add(resolver);
                }
              });
      return resolverDefinitionsByInput.entrySet().stream()
          .collect(toImmutableMap(Entry::getKey, e -> e.getValue().build()));
    }
  }
}
