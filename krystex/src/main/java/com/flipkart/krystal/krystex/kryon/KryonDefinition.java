package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.krystex.kryon.KryonUtils.toView;

import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;

/**
 * @param dependencyKryons Map of dependency name to kryonId.
 */
public record KryonDefinition(
    KryonId kryonId,
    KryonLogicId mainLogicId,
    ImmutableMap<String, KryonId> dependencyKryons,
    ImmutableList<ResolverDefinition> resolverDefinitions,
    Optional<KryonLogicId> multiResolverLogicId,
    KryonDefinitionView kryonDefinitionView,
    KryonDefinitionRegistry kryonDefinitionRegistry) {

  public KryonDefinition(
      KryonId kryonId,
      KryonLogicId mainLogicId,
      ImmutableMap<String, KryonId> dependencyKryons,
      ImmutableList<ResolverDefinition> resolverDefinitions,
      Optional<KryonLogicId> multiResolverLogicId,
      KryonDefinitionRegistry kryonDefinitionRegistry) {
    this(
        kryonId,
        mainLogicId,
        dependencyKryons,
        resolverDefinitions,
        multiResolverLogicId,
        toView(resolverDefinitions, dependencyKryons),
        kryonDefinitionRegistry);
  }

  public <T> MainLogicDefinition<T> getMainLogicDefinition() {
    return kryonDefinitionRegistry().logicDefinitionRegistry().getMain(mainLogicId());
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
  record KryonDefinitionView(
      ImmutableMap<Optional<String>, ImmutableSet<ResolverDefinition>> resolverDefinitionsByInput,
      ImmutableMap<String, ImmutableSet<ResolverDefinition>> resolverDefinitionsByDependencies,
      ImmutableSet<String> dependenciesWithNoResolvers) {}
}
