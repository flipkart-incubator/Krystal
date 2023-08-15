package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
    KryonDefinitionRegistry kryonDefinitionRegistry) {

  public <T> MainLogicDefinition<T> getMainLogicDefinition() {
    return kryonDefinitionRegistry().logicDefinitionRegistry().getMain(mainLogicId());
  }
}
