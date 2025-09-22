package com.flipkart.krystal.krystex.kryon;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.facets.FacetUtils;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.resolution.Resolver;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Useful data views over {@link VajramKryonDefinition}'s data
 *
 * @param resolverDefinitionsBySource Maps each input with a set of resolver definitions that
 *     consume that input. {@link Optional#empty()} is mapped to those resolver definitions which do
 *     not take any inputs.
 * @param resolverDefinitionsByDependencies Maps each dependency with a set of resolverDefinitions
 *     which resolve inputs of that dependency
 * @param dependenciesWithNoResolvers Set of dependency names which have no resolvers.
 */
record KryonDefinitionView(
    ImmutableMap<Integer, Facet> facetsById,
    ImmutableMap<FacetType, ImmutableSet<Facet>> facetsByType,
    ImmutableSet<Facet> givenFacets,
    ImmutableSet<Dependency> dependencies,
    ImmutableMap<Optional<Facet>, ImmutableSet<Resolver>> resolverDefinitionsBySource,
    ImmutableMap<Dependency, ImmutableSet<Resolver>> resolverDefinitionsByDependencies,
    ImmutableSet<Dependency> dependenciesWithNoResolvers,
    ImmutableSet<Dependency> dependenciesWithNoFacetResolvers,
    ImmutableMap<Dependency, ImmutableSet<Facet>> dependencyToBoundFacetsMapping,
    ImmutableMap<Facet, ImmutableSet<Dependency>> dependenciesByBoundFacet) {
  static KryonDefinitionView createView(
      Set<? extends Facet> allFacets,
      ImmutableMap<ResolverDefinition, Resolver> resolversByDefinition) {
    ImmutableMap<Dependency, ImmutableSet<Resolver>> resolverDefinitionsByDependencies =
        ImmutableMap.copyOf(
            resolversByDefinition.values().stream()
                .collect(groupingBy(d -> d.definition().target().dependency(), toImmutableSet())));
    Map<FacetType, Set<Facet>> facetsByType = new LinkedHashMap<>();
    for (Facet facet : allFacets) {
      facetsByType.computeIfAbsent(facet.facetType(), _t -> new LinkedHashSet<>()).add(facet);
    }
    List<Dependency> list = new ArrayList<>();
    for (Facet f : facetsByType.getOrDefault(FacetType.DEPENDENCY, ImmutableSet.of())) {
      if (f instanceof Dependency dependency) {
        list.add(dependency);
      }
    }
    ImmutableSet<Dependency> dependencies = ImmutableSet.copyOf(list);
    ImmutableSet<Dependency> dependenciesWithNoResolvers =
        dependencies.stream()
            .filter(
                depName ->
                    resolverDefinitionsByDependencies
                        .getOrDefault(depName, ImmutableSet.of())
                        .isEmpty())
            .collect(toImmutableSet());
    ImmutableMap<Optional<Facet>, ImmutableSet<ResolverDefinition>> resolverDefinitionsByFacets =
        createResolverDefinitionsByFacets(resolversByDefinition.keySet());
    return new KryonDefinitionView(
        allFacets.stream().collect(toImmutableMap(Facet::id, Function.identity())),
        facetsByType.entrySet().stream()
            .collect(toImmutableMap(Entry::getKey, e -> ImmutableSet.copyOf(e.getValue()))),
        allFacets.stream().filter(FacetUtils::isGiven).collect(toImmutableSet()),
        dependencies,
        createResolverDefinitionsBySource(resolversByDefinition),
        resolverDefinitionsByDependencies,
        dependenciesWithNoResolvers,
        resolverDefinitionsByFacets
            .getOrDefault(Optional.<Facet>empty(), ImmutableSet.of())
            .stream()
            .map(rd -> rd.target().dependency())
            .collect(toImmutableSet()),
        getDependencyToBoundFacetsMapping(resolverDefinitionsByDependencies),
        getDependenciesByBoundFacet(resolverDefinitionsByFacets));
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

  private static ImmutableMap<Optional<Facet>, ImmutableSet<ResolverDefinition>>
      createResolverDefinitionsByFacets(
          ImmutableCollection<ResolverDefinition> resolverDefinitions) {
    Map<Optional<Facet>, Builder<ResolverDefinition>> resolverDefinitionsBySource =
        new LinkedHashMap<>();
    resolverDefinitions.forEach(
        resolverDefinition -> {
          if (!resolverDefinition.sources().isEmpty()) {
            resolverDefinition
                .sources()
                .forEach(
                    input ->
                        resolverDefinitionsBySource
                            .computeIfAbsent(Optional.of(input), s -> ImmutableSet.builder())
                            .add(resolverDefinition));
          } else {
            resolverDefinitionsBySource
                .computeIfAbsent(Optional.empty(), s -> ImmutableSet.builder())
                .add(resolverDefinition);
          }
        });
    return resolverDefinitionsBySource.entrySet().stream()
        .collect(toImmutableMap(Entry::getKey, e -> e.getValue().build()));
  }

  private static ImmutableMap<Dependency, ImmutableSet<Facet>> getDependencyToBoundFacetsMapping(
      ImmutableMap<Dependency, ImmutableSet<Resolver>> resolverDefinitionsByDependencies) {
    Map<Dependency, ImmutableSet<Facet>> dependencyToBoundFacetsMapping = new LinkedHashMap<>();
    for (Entry<Dependency, ImmutableSet<Resolver>> e :
        resolverDefinitionsByDependencies.entrySet()) {
      Dependency depName = e.getKey();
      ImmutableSet<Resolver> resolvers = e.getValue();
      Set<Facet> boundFromInputs = new LinkedHashSet<>();
      for (Resolver resolver : resolvers) {
        boundFromInputs.addAll(resolver.definition().sources());
      }
      dependencyToBoundFacetsMapping.put(depName, ImmutableSet.copyOf(boundFromInputs));
    }
    return ImmutableMap.copyOf(dependencyToBoundFacetsMapping);
  }

  private static ImmutableMap<Facet, ImmutableSet<Dependency>> getDependenciesByBoundFacet(
      ImmutableMap<Optional<Facet>, ImmutableSet<ResolverDefinition>> resolverDefinitionsByFacets) {
    Map<Facet, ImmutableSet<Dependency>> dependenciesByBoundFacet = new LinkedHashMap<>();
    for (Entry<Optional<Facet>, ImmutableSet<ResolverDefinition>> e :
        resolverDefinitionsByFacets.entrySet()) {
      Optional<Facet> facetNameOpt = e.getKey();
      if (facetNameOpt.isEmpty()) {
        continue;
      }
      Facet facetName = facetNameOpt.get();
      Set<Dependency> dependenciesForFacet = new LinkedHashSet<>();
      for (ResolverDefinition resolverDefinition : e.getValue()) {
        dependenciesForFacet.add(resolverDefinition.target().dependency());
      }
      dependenciesByBoundFacet.put(facetName, ImmutableSet.copyOf(dependenciesForFacet));
    }
    return ImmutableMap.copyOf(dependenciesByBoundFacet);
  }
}
