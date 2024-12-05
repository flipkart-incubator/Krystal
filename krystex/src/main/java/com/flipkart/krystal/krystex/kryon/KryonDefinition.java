package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.krystex.kryon.FacetType.DEPENDENCY;
import static com.flipkart.krystal.krystex.kryon.FacetType.INPUT;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;

import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

/**
 * @param dependencyKryons Map of dependency name to kryonId.
 */
public record KryonDefinition(
    KryonId kryonId,
    ImmutableSet<String> inputs,
    KryonLogicId outputLogicId,
    ImmutableMap<String, KryonId> dependencyKryons,
    ImmutableList<ResolverDefinition> resolverDefinitions,
    Optional<KryonLogicId> multiResolverLogicId,
    KryonDefinitionRegistry kryonDefinitionRegistry,
    KryonDefinitionView view,
    ElementTags tags) {

  public KryonDefinition(
      KryonId kryonId,
      Set<String> inputs,
      KryonLogicId outputLogicId,
      ImmutableMap<String, KryonId> dependencyKryons,
      ImmutableList<ResolverDefinition> resolverDefinitions,
      Optional<KryonLogicId> multiResolverLogicId,
      KryonDefinitionRegistry kryonDefinitionRegistry,
      ElementTags tags) {
    this(
        kryonId,
        ImmutableSet.copyOf(inputs),
        outputLogicId,
        dependencyKryons,
        resolverDefinitions,
        multiResolverLogicId,
        kryonDefinitionRegistry,
        KryonDefinitionView.createView(inputs, resolverDefinitions, dependencyKryons),
        tags);
  }

  public <T> OutputLogicDefinition<T> getOutputLogicDefinition() {
    return kryonDefinitionRegistry().logicDefinitionRegistry().getOutputLogic(outputLogicId());
  }

  public ImmutableSet<String> facetsByType(FacetType facetType) {
    return view.facetsByType().getOrDefault(facetType, ImmutableSet.of());
  }

  public ImmutableMap<Optional<String>, ImmutableSet<ResolverDefinition>>
      resolverDefinitionsByInput() {
    return view.resolverDefinitionsByInput();
  }

  public ImmutableSet<String> dependenciesWithNoResolvers() {
    return view.dependenciesWithNoResolvers();
  }

  public ImmutableMap<String, ImmutableSet<ResolverDefinition>>
      resolverDefinitionsByDependencies() {
    return view.resolverDefinitionsByDependencies();
  }

  public ImmutableSet<String> facetNames() {
    return view.facetNames();
  }

  /**
   * Useful data views over {@link KryonDefinition}'s data
   *
   * @param resolverDefinitionsByInput Maps each input with a set of resolver definitions that
   *     consume that input. {@link Optional#empty()} is mapped to those resolver definitions which
   *     do not take any inputs.
   * @param resolverDefinitionsByDependencies Maps each dependency with a set of resolverDefinitions
   *     which resolve inputs of that dependency
   * @param dependenciesWithNoResolvers Set of dependency names which have no resolvers.
   */
  private record KryonDefinitionView(
      ImmutableMap<FacetType, ImmutableSet<String>> facetsByType,
      ImmutableSet<String> facetNames,
      ImmutableMap<Optional<String>, ImmutableSet<ResolverDefinition>> resolverDefinitionsByInput,
      ImmutableMap<String, ImmutableSet<ResolverDefinition>> resolverDefinitionsByDependencies,
      ImmutableSet<String> dependenciesWithNoResolvers) {
    private static KryonDefinitionView createView(
        Set<String> inputs,
        ImmutableList<ResolverDefinition> resolverDefinitions,
        ImmutableMap<String, KryonId> dependencyKryons) {
      ImmutableMap<String, ImmutableSet<ResolverDefinition>> resolverDefinitionsByDependencies =
          ImmutableMap.copyOf(
              resolverDefinitions.stream()
                  .collect(groupingBy(ResolverDefinition::dependencyName, toImmutableSet())));
      ImmutableSet<String> dependencyFacets = dependencyKryons.keySet();
      ImmutableSet<String> dependenciesWithNoResolvers =
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
          ImmutableSet.<String>builder().addAll(inputs).addAll(dependencyKryons.keySet()).build(),
          createResolverDefinitionsByInputs(resolverDefinitions),
          resolverDefinitionsByDependencies,
          dependenciesWithNoResolvers);
    }

    private static ImmutableMap<Optional<String>, ImmutableSet<ResolverDefinition>>
        createResolverDefinitionsByInputs(ImmutableList<ResolverDefinition> resolverDefinitions) {
      Map<Optional<String>, Builder<ResolverDefinition>> resolverDefinitionsByInput =
          new LinkedHashMap<>();
      resolverDefinitions.forEach(
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
