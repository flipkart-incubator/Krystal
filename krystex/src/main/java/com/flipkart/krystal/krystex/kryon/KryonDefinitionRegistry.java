package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class KryonDefinitionRegistry {

  private final LogicDefinitionRegistry logicDefinitionRegistry;
  private final Map<VajramID, KryonDefinition> kryonDefinitions = new ConcurrentHashMap<>();
  private final DependantChainStart dependantChainStart = new DependantChainStart();

  public KryonDefinitionRegistry(LogicDefinitionRegistry logicDefinitionRegistry) {
    this.logicDefinitionRegistry = logicDefinitionRegistry;
  }

  public LogicDefinitionRegistry logicDefinitionRegistry() {
    return logicDefinitionRegistry;
  }

  public KryonDefinition get(VajramID vajramID) {
    KryonDefinition kryon = kryonDefinitions.get(vajramID);
    if (kryon == null) {
      throw new IllegalArgumentException("No Kryon with id %s found".formatted(vajramID));
    }
    return kryon;
  }

  public KryonDefinition newKryonDefinition(
      String kryonId,
      Set<? extends Facet> facets,
      KryonLogicId outputLogicId,
      ImmutableMap<Dependency, VajramID> dependencyKryons,
      ImmutableMap<ResolverDefinition, Resolver> resolversByDefinition,
      LogicDefinition<CreateNewRequest> createNewRequest,
      LogicDefinition<FacetsFromRequest> facetsFromRequest,
      ElementTags tags) {
    KryonDefinition kryonDefinition =
        new KryonDefinition(
            new VajramID(kryonId),
            facets,
            outputLogicId,
            dependencyKryons,
            resolversByDefinition,
            createNewRequest,
            facetsFromRequest,
            this,
            tags);
    kryonDefinitions.put(kryonDefinition.vajramID(), kryonDefinition);
    return kryonDefinition;
  }

  public DependantChain getDependantChainsStart() {
    return dependantChainStart;
  }
}
