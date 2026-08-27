package com.flipkart.krystal.krystex.epochs;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.krystex.DependentChainDisabler;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.epochs.LogicSet.DepResolvers;
import com.flipkart.krystal.krystex.epochs.LogicSet.OutputLogics;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.traits.StaticDispatchPolicy;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;

public record EpochGroups(ImmutableMap<VajramID, VajramEpochGroups> vajramEpochGroups) {

  public static EpochGroups computeEpochGroups(
      VajramGraph graph,
      TraitDispatchPolicies traitDispatchPolicies,
      DependentChainDisabler dependentChainDisabler,
      Collection<VajramID> externallyInvocableVajramIds) {
    List<VajramDefinition> externallyInvocableVajrams =
        externallyInvocableVajramIds.stream()
            .map(graph::tryGetVajramDefinition)
            .filter(Objects::nonNull)
            .toList();
    Map<VajramID, Map<Integer, Set<DependentChain>>> vajramToOrdinalChains = new HashMap<>();
    Map<VajramID, Integer> vajramsToOutgoingOrdinals = new HashMap<>();
    for (VajramDefinition vajramDefinition : externallyInvocableVajrams) {
      Collection<VajramID> dispatchTargets =
          getDispatchTargets(vajramDefinition.vajramId(), graph, traitDispatchPolicies, null);
      for (VajramID dispatchTargetID : dispatchTargets) {
        collateDepChainOrdinals(
            vajramToOrdinalChains,
            vajramsToOutgoingOrdinals,
            graph,
            dispatchTargetID,
            graph.kryonDefinitionRegistry().getDependentChainsStart(),
            0,
            dependentChainDisabler,
            traitDispatchPolicies);
      }
    }
    return createEpochGroups(vajramToOrdinalChains);
  }

  static Collection<VajramID> getDispatchTargets(
      VajramID depVajramID,
      VajramGraph graph,
      TraitDispatchPolicies traitDispatchPolicies,
      Dependency dependency) {
    VajramDefinition depVajramDef = graph.tryGetVajramDefinition(depVajramID);
    if (depVajramDef == null) {
      return List.of();
    }
    Collection<VajramID> depVajramIDs = new ArrayList<>();
    if (depVajramDef.isTrait()) {
      TraitDispatchPolicy traitDispatchPolicy = traitDispatchPolicies.get(depVajramID);
      if (traitDispatchPolicy == null) {
        throw new IllegalStateException(
            "Trait "
                + depVajramID
                + " does not have a trait dispatch policy defined. Cannot auto-compute batcher config.");
      }
      if (dependency != null
          && traitDispatchPolicy instanceof StaticDispatchPolicy staticDispatchPolicy) {
        depVajramIDs.add(staticDispatchPolicy.getDispatchTargetID(dependency));
      } else {
        for (VajramID vajramID : traitDispatchPolicy.dispatchTargetIDs()) {
          depVajramIDs.addAll(
              getDispatchTargets(vajramID, graph, traitDispatchPolicies, dependency));
        }
      }
    } else {
      depVajramIDs = List.of(depVajramID);
    }
    return depVajramIDs;
  }

  /**
   * Given A LogicSet of a given vajram (ex: resolvers of a dependency or output logics) and an
   * incoming ordinal, this method computes the effective ordinal of all the facets which are the
   * sources to the logic set.
   *
   * <p>For example: if a vajram has an output logic which has two sources which are dependency
   * vajrams with response ordinals 1 and 2 respectively, then the source ordinal of the output
   * logic will be
   *
   * <ul>
   *   <li>2 if the two dependencies are parallel (max 1,2)
   *   <li>3 if the two dependencies are sequential (1 + 2)
   * </ul>
   *
   * @param sourceOrdinalKey
   * @param sourceOrdinals
   * @param vajramsToResponseOrdinals
   * @param graph
   * @param dependentChainDisabler
   * @param traitDispatchPolicies
   * @return
   */
  static int computeSourceOrdinal(
      SourceOrdinalKey sourceOrdinalKey,
      Map<SourceOrdinalKey, Integer> sourceOrdinals,
      Map<VajramID, Integer> vajramsToResponseOrdinals,
      VajramGraph graph,
      DependentChainDisabler dependentChainDisabler,
      TraitDispatchPolicies traitDispatchPolicies) {
    if (sourceOrdinals.containsKey(sourceOrdinalKey)) {
      return sourceOrdinals.get(sourceOrdinalKey);
    }
    DependentChain incomingDepChain = sourceOrdinalKey.incomingDepChain();
    LogicSet logicSet = sourceOrdinalKey.logicSet();
    int incomingOrdinal = sourceOrdinalKey.incomingOrdinal();
    int sourceOrdinal = incomingOrdinal;
    for (Facet source : logicSet.sources(graph)) {
      if (source instanceof Dependency sourceDependency) {
        int i =
            computeSourceOrdinal(
                new SourceOrdinalKey(
                    new DepResolvers(sourceDependency), incomingOrdinal, incomingDepChain),
                sourceOrdinals,
                vajramsToResponseOrdinals,
                graph,
                dependentChainDisabler,
                traitDispatchPolicies);
        for (VajramID sourceDispatchTarget :
            getDispatchTargets(
                sourceDependency.onVajramID(), graph, traitDispatchPolicies, sourceDependency)) {
          sourceOrdinal =
              Math.max(
                  sourceOrdinal,
                  i
                      + computeResponseOrdinal(
                          sourceDispatchTarget,
                          sourceOrdinals,
                          vajramsToResponseOrdinals,
                          graph,
                          incomingDepChain.extend(sourceDependency.ofVajramID(), sourceDependency),
                          dependentChainDisabler,
                          traitDispatchPolicies));
        }
      }
    }
    sourceOrdinals.put(sourceOrdinalKey, sourceOrdinal);
    return sourceOrdinal;
  }

  /**
   * Given a vajram, computes the response ordinal of that vajram in isolation (irrespective of
   * where it's invoked from - i.e. assuming incoming ordinal is 0)
   *
   * @param vajramBeingInvokedID
   * @param sourceOrdinals
   * @param vajramsToResponseOrdinals
   * @param graph
   * @param incomingDepChain
   * @param dependentChainDisabler
   * @param traitDispatchPolicies
   */
  static int computeResponseOrdinal(
      VajramID vajramBeingInvokedID,
      Map<SourceOrdinalKey, Integer> sourceOrdinals,
      Map<VajramID, Integer> vajramsToResponseOrdinals,
      VajramGraph graph,
      DependentChain incomingDepChain,
      DependentChainDisabler dependentChainDisabler,
      TraitDispatchPolicies traitDispatchPolicies) {
    if (dependentChainDisabler.isDisabled(incomingDepChain)) {
      return 0;
    }
    if (!vajramsToResponseOrdinals.containsKey(vajramBeingInvokedID)) {
      int responseOrdinal =
          computeSourceOrdinal(
              new SourceOrdinalKey(new OutputLogics(vajramBeingInvokedID), 0, incomingDepChain),
              sourceOrdinals,
              vajramsToResponseOrdinals,
              graph,
              dependentChainDisabler,
              traitDispatchPolicies);
      if (Optional.ofNullable(graph.tryGetVajramDefinition(vajramBeingInvokedID))
              .map(VajramDefinition::def)
              .orElse(null)
          instanceof IOVajramDef<?>) {
        responseOrdinal++;
      }
      vajramsToResponseOrdinals.put(vajramBeingInvokedID, responseOrdinal);
    }
    return vajramsToResponseOrdinals.get(vajramBeingInvokedID);
  }

  /**
   * For every IO Vajram, this method collates all depChains ending in that IO Vajram by its ordinal
   *
   * @param vajramsToOrdinalChains a cache which maps a vajram to its epoch to set of depChains
   *     which map to that epoch
   * @param vajramsToResponseOrdinals a cache which maps a vajram to its response ordinals
   * @param graph
   * @param vajramIDBeingInvoked the vajram for which depchain ordinals need to be collated
   * @param incomingDepChain
   * @param incomingOrdinal the current ordinal at the invocation location from where this vajram is
   *     being invoked
   * @param dependentChainDisabler
   * @param traitDispatchPolicies
   */
  private static void collateDepChainOrdinals(
      Map<VajramID, Map<Integer, Set<DependentChain>>> vajramsToOrdinalChains,
      Map<VajramID, Integer> vajramsToResponseOrdinals,
      VajramGraph graph,
      VajramID vajramIDBeingInvoked,
      DependentChain incomingDepChain,
      int incomingOrdinal,
      DependentChainDisabler dependentChainDisabler,
      TraitDispatchPolicies traitDispatchPolicies) {
    if (dependentChainDisabler.isDisabled(incomingDepChain)) {
      return;
    }
    VajramDefinition vajramBeingInvoked = graph.tryGetVajramDefinition(vajramIDBeingInvoked);
    if (vajramBeingInvoked == null) {
      return;
    }
    if (vajramBeingInvoked.isTrait()) {
      throw new AssertionError(
          "collateDepChainOrdinals cannot be called for traits. First resolve dispatch targets before calling this method.");
    }
    List<Dependency> dependencies = getDependencies(vajramBeingInvoked);
    Map<SourceOrdinalKey, Integer> sourceOrdinals = new HashMap<>();
    for (Dependency dependency : dependencies) {
      DependentChain outgoingDepChain =
          incomingDepChain.extend(vajramBeingInvoked.vajramId(), dependency);
      for (VajramID depVajramID :
          getDispatchTargets(dependency.onVajramID(), graph, traitDispatchPolicies, dependency)) {
        collateDepChainOrdinals(
            vajramsToOrdinalChains,
            vajramsToResponseOrdinals,
            graph,
            depVajramID,
            outgoingDepChain,
            computeSourceOrdinal(
                new SourceOrdinalKey(
                    new DepResolvers(dependency), incomingOrdinal, outgoingDepChain),
                sourceOrdinals,
                vajramsToResponseOrdinals,
                graph,
                dependentChainDisabler,
                traitDispatchPolicies),
            dependentChainDisabler,
            traitDispatchPolicies);
      }
    }

    final int depChainOrdinal =
        incomingOrdinal
            + computeResponseOrdinal(
                vajramIDBeingInvoked,
                sourceOrdinals,
                vajramsToResponseOrdinals,
                graph,
                incomingDepChain,
                dependentChainDisabler,
                traitDispatchPolicies);
    vajramsToOrdinalChains
        .computeIfAbsent(vajramIDBeingInvoked, _vid -> new HashMap<>())
        .computeIfAbsent(depChainOrdinal, _depth -> new HashSet<>())
        .add(incomingDepChain);
  }

  static List<Dependency> getDependencies(VajramDefinition vajramBeingInvoked) {
    List<Dependency> dependencies =
        vajramBeingInvoked.facetSpecs().stream()
            .filter(f -> f instanceof Dependency)
            .<@NonNull Dependency>map(Dependency.class::cast)
            .toList();
    return dependencies;
  }

  private static EpochGroups createEpochGroups(
      Map<VajramID, Map<Integer, Set<DependentChain>>> vajramConfigs) {
    Map<VajramID, VajramEpochGroups> depChainEpochGroupsByVajram = new LinkedHashMap<>();
    vajramConfigs.forEach(
        (vajramId, vajramConfig) -> {
          Map<Integer, EpochGroup> epochGroups = new LinkedHashMap<>();
          for (Entry<Integer, Set<DependentChain>> entry : vajramConfig.entrySet()) {
            epochGroups.put(
                entry.getKey(),
                new EpochGroup(vajramId, entry.getKey(), ImmutableSet.copyOf(entry.getValue())));
          }
          depChainEpochGroupsByVajram.put(
              vajramId, new VajramEpochGroups(ImmutableMap.copyOf(epochGroups)));
        });
    return new EpochGroups(ImmutableMap.copyOf(depChainEpochGroupsByVajram));
  }

  record SourceOrdinalKey(
      LogicSet logicSet, int incomingOrdinal, DependentChain incomingDepChain) {}
}
