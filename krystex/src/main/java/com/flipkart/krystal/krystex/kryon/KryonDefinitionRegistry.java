package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class KryonDefinitionRegistry {

  private final LogicDefinitionRegistry logicDefinitionRegistry;
  private final Map<KryonId, KryonDefinition> kryonDefinitions = new LinkedHashMap<>();
  private final DependantChainStart dependantChainStart = new DependantChainStart();

  public KryonDefinitionRegistry(LogicDefinitionRegistry logicDefinitionRegistry) {
    this.logicDefinitionRegistry = logicDefinitionRegistry;
  }

  public LogicDefinitionRegistry logicDefinitionRegistry() {
    return logicDefinitionRegistry;
  }

  public KryonDefinition get(KryonId kryonId) {
    KryonDefinition kryon = kryonDefinitions.get(kryonId);
    if (kryon == null) {
      throw new IllegalArgumentException("No Kryon with id %s found".formatted(kryonId));
    }
    return kryon;
  }

  public KryonDefinition newKryonDefinition(String kryonId, KryonLogicId outputLogicId) {
    return newKryonDefinition(kryonId, outputLogicId, ImmutableMap.of());
  }

  public KryonDefinition newKryonDefinition(
      String kryonId, KryonLogicId outputLogicId, ImmutableMap<String, KryonId> dependencyKryons) {
    return newKryonDefinition(kryonId, outputLogicId, dependencyKryons, ImmutableList.of(), null);
  }

  public KryonDefinition newKryonDefinition(
      String kryonId,
      KryonLogicId outputLogicId,
      ImmutableMap<String, KryonId> dependencyKryons,
      ImmutableList<ResolverDefinition> resolverDefinitions,
      @Nullable KryonLogicId mulitResolverId) {
    if (!resolverDefinitions.isEmpty() && mulitResolverId == null) {
      throw new IllegalArgumentException("missing multi resolver logic");
    }
    KryonDefinition kryonDefinition =
        new KryonDefinition(
            new KryonId(kryonId),
            outputLogicId,
            dependencyKryons,
            resolverDefinitions,
            Optional.ofNullable(mulitResolverId),
            this);
    kryonDefinitions.put(kryonDefinition.kryonId(), kryonDefinition);
    return kryonDefinition;
  }

  public DependantChain getDependantChainsStart() {
    return dependantChainStart;
  }
}
