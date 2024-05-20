package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;
import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;

import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.krystex.resolution.FacetsFromRequest;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

/**
 * A stateless, reusable definition of a Kryon
 *
 * @param dependencyKryons Map of dependency name to kryonId.
 */
public record KryonDefinition(
    KryonId kryonId,
    ImmutableSet<Integer> inputs,
    KryonLogicId outputLogicId,
    ImmutableMap<Integer, KryonId> dependencyKryons,
    ImmutableMap</*ResolverId*/ Integer, ResolverDefinition> resolverDefinitionsById,
    Optional<KryonLogicId> multiResolverLogicId,
    LogicDefinition<CreateNewRequest> createNewRequest,
    LogicDefinition<FacetsFromRequest> facetsFromRequest,
    KryonDefinitionRegistry kryonDefinitionRegistry,
    KryonDefinitionView view) {

  public KryonDefinition(
      KryonId kryonId,
      Set<Integer> inputs,
      KryonLogicId outputLogicId,
      ImmutableMap<Integer, KryonId> dependencyKryons,
      ImmutableMap</*ResolverId*/ Integer, ResolverDefinition> resolverDefinitions,
      Optional<KryonLogicId> multiResolverLogicId,
      LogicDefinition<CreateNewRequest> createNewRequest,
      LogicDefinition<FacetsFromRequest> facetsFromRequest,
      KryonDefinitionRegistry kryonDefinitionRegistry) {
    this(
        kryonId,
        ImmutableSet.copyOf(inputs),
        outputLogicId,
        dependencyKryons,
        resolverDefinitions,
        multiResolverLogicId,
        createNewRequest,
        facetsFromRequest,
        kryonDefinitionRegistry,
        KryonDefinitionView.createView(inputs, resolverDefinitions, dependencyKryons));
  }

  public <T> OutputLogicDefinition<T> getOutputLogicDefinition() {
    return kryonDefinitionRegistry().logicDefinitionRegistry().getOutputLogic(outputLogicId());
  }

  public ImmutableSet<Integer> facetsByType(FacetType facetType) {
    return view.facetsByType().getOrDefault(facetType, ImmutableSet.of());
  }

  public ImmutableMap<Optional<Integer>, ImmutableSet<ResolverDefinition>>
      resolverDefinitionsByInput() {
    return view.resolverDefinitionsBySource();
  }

  public ImmutableSet<Integer> dependenciesWithNoResolvers() {
    return view.dependenciesWithNoResolvers();
  }

  public ImmutableMap<Integer, ImmutableSet<ResolverDefinition>>
      resolverDefinitionsByDependencies() {
    return view.resolverDefinitionsByDependencies();
  }

  public ImmutableSet<Integer> facetIds() {
    return view.facetIds();
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
      ImmutableMap<FacetType, ImmutableSet<Integer>> facetsByType,
      ImmutableSet<Integer> facetIds,
      ImmutableMap<Optional<Integer>, ImmutableSet<ResolverDefinition>> resolverDefinitionsBySource,
      ImmutableMap<Integer, ImmutableSet<ResolverDefinition>> resolverDefinitionsByDependencies,
      ImmutableSet<Integer> dependenciesWithNoResolvers) {
    private static KryonDefinitionView createView(
        Set<Integer> inputs,
        ImmutableMap<Integer, ResolverDefinition> resolverDefinitionsById,
        ImmutableMap<Integer, KryonId> dependencyKryons) {
      ImmutableMap<Integer, ImmutableSet<ResolverDefinition>> resolverDefinitionsByDependencies =
          ImmutableMap.copyOf(
              resolverDefinitionsById.values().stream()
                  .collect(groupingBy(ResolverDefinition::dependencyId, toImmutableSet())));
      ImmutableSet<Integer> dependencyFacets = dependencyKryons.keySet();
      ImmutableSet<Integer> dependenciesWithNoResolvers =
          dependencyFacets.stream()
              .filter(
                  depName ->
                      resolverDefinitionsByDependencies
                          .getOrDefault(depName, ImmutableSet.of())
                          .isEmpty())
              .collect(toImmutableSet());
      return new KryonDefinitionView(
          ImmutableMap.of(
              INPUT, ImmutableSet.copyOf(inputs), DEPENDENCY, dependencyKryons.keySet()),
          ImmutableSet.<Integer>builder().addAll(inputs).addAll(dependencyKryons.keySet()).build(),
          createResolverDefinitionsBySource(resolverDefinitionsById),
          resolverDefinitionsByDependencies,
          dependenciesWithNoResolvers);
    }

    private static ImmutableMap<Optional<Integer>, ImmutableSet<ResolverDefinition>>
        createResolverDefinitionsBySource(
            ImmutableMap<Integer, ResolverDefinition> resolverDefinitions) {
      Map<Optional<Integer>, ImmutableSet.Builder<ResolverDefinition>> resolverDefinitionsByInput =
          new LinkedHashMap<>();
      resolverDefinitions
          .values()
          .forEach(
              resolverDefinition -> {
                if (!resolverDefinition.boundFrom().isEmpty()) {
                  resolverDefinition
                      .boundFrom()
                      .forEach(
                          input ->
                              resolverDefinitionsByInput
                                  .computeIfAbsent(Optional.of(input), s -> ImmutableSet.builder())
                                  .add(resolverDefinition));
                } else {
                  resolverDefinitionsByInput
                      .computeIfAbsent(Optional.empty(), s -> ImmutableSet.builder())
                      .add(resolverDefinition);
                }
              });
      return resolverDefinitionsByInput.entrySet().stream()
          .collect(toImmutableMap(Entry::getKey, e -> e.getValue().build()));
    }
  }
}
