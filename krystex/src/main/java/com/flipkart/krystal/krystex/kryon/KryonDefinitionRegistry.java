package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.krystex.resolution.FacetsFromRequest;
import com.flipkart.krystal.krystex.resolution.Resolver;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  public KryonDefinition newKryonDefinition(
      String kryonId,
      Set<? extends Facet> facets,
      KryonLogicId outputLogicId,
      ImmutableMap<Dependency, KryonId> dependencyKryons,
      ImmutableMap<ResolverDefinition, Resolver> resolversByDefinition,
      LogicDefinition<CreateNewRequest> createNewRequest,
      LogicDefinition<FacetsFromRequest> facetsFromRequest,
      @Nullable KryonLogicId mulitResolverId,
      ElementTags tags) {
    if (!resolversByDefinition.isEmpty() && mulitResolverId == null) {
      throw new IllegalArgumentException("missing multi resolver logic");
    }
    KryonDefinition kryonDefinition =
        new KryonDefinition(
            new KryonId(kryonId),
            facets,
            outputLogicId,
            dependencyKryons,
            resolversByDefinition,
            Optional.ofNullable(mulitResolverId),
            createNewRequest,
            facetsFromRequest,
            this,
            tags);
    kryonDefinitions.put(kryonDefinition.kryonId(), kryonDefinition);
    return kryonDefinition;
  }

  public DependantChain getDependantChainsStart() {
    return dependantChainStart;
  }
}
