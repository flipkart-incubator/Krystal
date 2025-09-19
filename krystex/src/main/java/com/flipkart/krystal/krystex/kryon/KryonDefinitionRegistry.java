package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.GraphExecutionLogic;
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
import org.checkerframework.checker.nullness.qual.Nullable;

public final class KryonDefinitionRegistry {

  private final LogicDefinitionRegistry logicDefinitionRegistry;
  private final Map<VajramID, KryonDefinition> kryonDefinitions = new ConcurrentHashMap<>();
  private final DependentChainStart dependentChainStart = new DependentChainStart();

  public KryonDefinitionRegistry(LogicDefinitionRegistry logicDefinitionRegistry) {
    this.logicDefinitionRegistry = logicDefinitionRegistry;
  }

  public LogicDefinitionRegistry logicDefinitionRegistry() {
    return logicDefinitionRegistry;
  }

  public @Nullable KryonDefinition get(VajramID vajramID) {
    return kryonDefinitions.get(vajramID);
  }

  public KryonDefinition getOrThrow(VajramID vajramID) {
    KryonDefinition kryon = get(vajramID);
    if (kryon == null) {
      throw new IllegalArgumentException("No Kryon with id %s found".formatted(vajramID));
    }
    return kryon;
  }

  public VajramKryonDefinition newVajramKryonDefinition(
      VajramID vajramID,
      Set<? extends Facet> facets,
      KryonLogicId outputLogicId,
      ImmutableMap<ResolverDefinition, Resolver> resolversByDefinition,
      LogicDefinition<CreateNewRequest> createNewRequest,
      LogicDefinition<FacetsFromRequest> facetsFromRequest,
      GraphExecutionLogic graphExecutionLogic,
      ElementTags tags) {
    VajramKryonDefinition kryonDefinition =
        new VajramKryonDefinition(
            vajramID,
            facets,
            outputLogicId,
            resolversByDefinition,
            createNewRequest,
            facetsFromRequest,
            this,
            graphExecutionLogic,
            tags);
    kryonDefinitions.put(kryonDefinition.vajramID(), kryonDefinition);
    return kryonDefinition;
  }

  public TraitKryonDefinition newTraitKryonDefinition(
      String kryonId,
      Set<? extends Facet> facets,
      LogicDefinition<CreateNewRequest> createNewRequest,
      ElementTags tags) {
    TraitKryonDefinition kryonDefinition =
        new TraitKryonDefinition(new VajramID(kryonId), facets, createNewRequest, this, tags);
    kryonDefinitions.put(kryonDefinition.vajramID(), kryonDefinition);
    return kryonDefinition;
  }

  public DependentChain getDependentChainsStart() {
    return dependentChainStart;
  }
}
