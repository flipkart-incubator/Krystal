package com.flipkart.krystal.vajramexecutor.krystex.batching;

import static java.lang.Math.max;
import static java.util.stream.Collectors.groupingBy;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.kryon.DefaultDependentChain;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.DependentChainStart;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig.OutputLogicDecoratorContext;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.batching.InputBatcher;
import com.flipkart.krystal.vajram.batching.InputBatcherImpl;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import com.google.common.collect.ImmutableList;
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
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;

public record DepChainBatcherConfig(
    Predicate<LogicExecutionContext> shouldBatch,
    Function<LogicExecutionContext, String> instanceIdGenerator,
    Function<OutputLogicDecoratorContext, OutputLogicDecorator> decoratorFactory) {

  public static final DepChainBatcherConfig NO_BATCHING =
      new DepChainBatcherConfig(_l -> false, _l -> "", _l -> OutputLogicDecorator.NO_OP);

  /**
   * Creates a default InputBatcherConfig which guarantees that every unique {@link DependentChain}
   * of a vajram gets its own {@link InputBatchingDecorator} and its own corresponding {@link
   * InputBatcher}. The instance id corresponding to a particular {@link DependentChain} is of the
   * form:
   *
   * <p>{@code [Start]>vajramId_1:dep_1>vajramId_2:dep_2>....>vajramId_n:dep_n}
   *
   * @param inputBatcherSupplier Supplies the {@link InputBatcher} corresponding to an {@link
   *     InputBatchingDecorator}. This supplier is guaranteed to be called exactly once for every
   *     unique {@link InputBatchingDecorator} instance.
   */
  public static DepChainBatcherConfig simple(Supplier<InputBatcher> inputBatcherSupplier) {
    return new DepChainBatcherConfig(
        logicExecutionContext -> true,
        logicExecutionContext -> generateInstanceId(logicExecutionContext.dependents()).toString(),
        outputLogicDecoratorContext ->
            new InputBatchingDecorator(
                outputLogicDecoratorContext.instanceId(),
                inputBatcherSupplier.get(),
                dependantChain ->
                    outputLogicDecoratorContext
                        .logicExecutionContext()
                        .dependents()
                        .equals(dependantChain)));
  }

  public static DepChainBatcherConfig sharedBatcher(
      Supplier<InputBatcher> inputBatcherSupplier,
      String instanceId,
      DependentChain... dependentChains) {
    return sharedBatcher(inputBatcherSupplier, instanceId, ImmutableSet.copyOf(dependentChains));
  }

  public static DepChainBatcherConfig sharedBatcher(
      Supplier<InputBatcher> inputBatcherSupplier,
      String instanceId,
      ImmutableSet<DependentChain> dependentChains) {
    return new DepChainBatcherConfig(
        logicExecutionContext -> dependentChains.contains(logicExecutionContext.dependents()),
        logicExecutionContext -> instanceId,
        outputLogicDecoratorContext ->
            new InputBatchingDecorator(
                instanceId, inputBatcherSupplier.get(), dependentChains::contains));
  }

  private static InputBatcherConfig registerBatchers(
      Map<VajramID, Map<Integer, Set<DependentChain>>> ioNodes,
      BatchSizeSupplier batchSizeSupplier,
      VajramGraph graph) {
    Map<VajramID, ImmutableList<DepChainBatcherConfig>> depChainBatcherConfigs =
        new LinkedHashMap<>();
    ioNodes.forEach(
        (vajramId, ioNodeMap) -> {
          if (isBatchingNeededForIoVajram(graph, vajramId)) {
            List<DepChainBatcherConfig> inputModulatorConfigs = new ArrayList<>(ioNodeMap.size());
            for (Entry<Integer, Set<DependentChain>> entry : ioNodeMap.entrySet()) {
              int depth = entry.getKey();
              Set<DependentChain> depChains = entry.getValue();
              inputModulatorConfigs.add(
                  DepChainBatcherConfig.sharedBatcher(
                      () -> new InputBatcherImpl(batchSizeSupplier.getBatchSize(vajramId)),
                      vajramId.id() + ":depth(" + depth + ")",
                      depChains.toArray(DependentChain[]::new)));
            }
            depChainBatcherConfigs.put(vajramId, ImmutableList.copyOf(inputModulatorConfigs));
          }
        });
    return new InputBatcherConfig(ImmutableMap.copyOf(depChainBatcherConfigs));
  }

  /**
   * @return decorator instanceId of the form {@code
   *     [Start]>vajramId_1:dep_1>vajramId_2:dep_2>....>vajramId_n:dep_n}
   */
  private static StringBuilder generateInstanceId(DependentChain dependentChain) {
    if (dependentChain instanceof DependentChainStart dependantChainStart) {
      return new StringBuilder(dependantChainStart.toString());
    } else if (dependentChain instanceof DefaultDependentChain defaultDependantChain) {
      if (defaultDependantChain.incomingDependentChain() instanceof DependentChainStart) {
        return generateInstanceId(defaultDependantChain.incomingDependentChain())
            .append('>')
            .append(defaultDependantChain.kryonId().id())
            .append(':')
            .append(defaultDependantChain.latestDependency());
      } else {
        return generateInstanceId(defaultDependantChain.incomingDependentChain())
            .append('>')
            .append(defaultDependantChain.latestDependency());
      }
    }
    throw new UnsupportedOperationException();
  }

  private static boolean isBatchingNeededForIoVajram(VajramGraph graph, VajramID ioNode) {
    VajramDefinition ioNodeVajram = graph.getVajramDefinition(ioNode);
    for (FacetSpec facetSpec : ioNodeVajram.facetSpecs()) {
      if (facetSpec.isBatched()) {
        return true;
      }
    }
    return false;
  }

  @FunctionalInterface
  public interface BatchSizeSupplier {
    int getBatchSize(VajramID vajramId);
  }

  public static InputBatcherConfig computeSharedBatcherConfig(
      VajramGraph graph, BatchSizeSupplier batchSizeSupplier) {
    return computeSharedBatcherConfig(graph, batchSizeSupplier, new TraitDispatchPolicies());
  }

  public static InputBatcherConfig computeSharedBatcherConfig(
      VajramGraph graph,
      BatchSizeSupplier batchSizeSupplier,
      TraitDispatchPolicies traitDispatchPolicies) {
    return computeSharedBatcherConfig(
        graph, batchSizeSupplier, traitDispatchPolicies, ImmutableSet.of());
  }

  public static InputBatcherConfig computeSharedBatcherConfig(
      VajramGraph graph,
      BatchSizeSupplier batchSizeSupplier,
      TraitDispatchPolicies traitDispatchPolicies,
      ImmutableSet<DependentChain> disabledDependentChains) {
    List<VajramDefinition> externallyInvocableVajrams =
        graph.vajramDefinitions().values().stream()
            .filter(
                v -> v.vajramTags().getAnnotationByType(InvocableOutsideGraph.class).isPresent())
            .toList();
    Map<VajramID, Map<Integer, Set<DependentChain>>> ioVajramsToOrdinalChains = new HashMap<>();
    Map<VajramID, Integer> vajramsToOutgoingOrdinals = new HashMap<>();
    for (VajramDefinition vajramDefinition : externallyInvocableVajrams) {
      collateDepChainOrdinals(
          ioVajramsToOrdinalChains,
          vajramsToOutgoingOrdinals,
          graph,
          vajramDefinition.vajramId(),
          graph.kryonDefinitionRegistry().getDependentChainsStart(),
          0,
          disabledDependentChains,
          traitDispatchPolicies);
    }
    return registerBatchers(ioVajramsToOrdinalChains, batchSizeSupplier, graph);
  }

  private static void collateDepChainOrdinals(
      Map<VajramID, Map<Integer, Set<DependentChain>>> ioVajramsToOrdinalChains,
      Map<VajramID, Integer> vajramsToResponseOrdinals,
      VajramGraph graph,
      VajramID vajramIDBeingInvoked,
      DependentChain incomingDepChain,
      int incomingOrdinal,
      ImmutableSet<DependentChain> disabledDependentChains,
      TraitDispatchPolicies traitDispatchPolicies) {
    if (disabledDependentChains.contains(incomingDepChain)) {
      return;
    }
    VajramDefinition vajramBeingInvoked = graph.getVajramDefinition(vajramIDBeingInvoked);
    List<Dependency> dependencies = getDependencies(vajramBeingInvoked);
    Map<Dependency, Integer> dependencyOrdinals = new HashMap<>();
    for (Dependency dependency : dependencies) {
      DependentChain outgoingDepChain =
          incomingDepChain.extend(vajramBeingInvoked.vajramId(), dependency);
      for (VajramID depVajramID :
          getDispatchTargets(dependency.onVajramID(), graph, traitDispatchPolicies)) {
        int depSourceOrdinal =
            computeSourceOrdinal(
                dependency,
                incomingOrdinal,
                dependencyOrdinals,
                vajramsToResponseOrdinals,
                graph,
                traitDispatchPolicies);
        collateDepChainOrdinals(
            ioVajramsToOrdinalChains,
            vajramsToResponseOrdinals,
            graph,
            depVajramID,
            outgoingDepChain,
            depSourceOrdinal,
            disabledDependentChains,
            traitDispatchPolicies);
      }
    }

    if (vajramBeingInvoked.def() instanceof IOVajramDef<?>) {
      final int depChainOrdinal =
          incomingOrdinal
              + computeResponseOrdinal(
                  vajramIDBeingInvoked, vajramsToResponseOrdinals, graph, traitDispatchPolicies);
      ioVajramsToOrdinalChains
          .computeIfAbsent(vajramIDBeingInvoked, _vid -> new HashMap<>())
          .computeIfAbsent(depChainOrdinal, _depth -> new HashSet<>())
          .add(incomingDepChain);
    }
  }

  private static @NonNull List<Dependency> getDependencies(VajramDefinition vajramBeingInvoked) {
    List<Dependency> dependencies =
        vajramBeingInvoked.facetSpecs().stream()
            .filter(f -> f instanceof Dependency)
            .<@NonNull Dependency>map(Dependency.class::cast)
            .toList();
    return dependencies;
  }

  private static Collection<VajramID> getDispatchTargets(
      VajramID depVajramID, VajramGraph graph, TraitDispatchPolicies traitDispatchPolicies) {
    VajramDefinition depVajramDef = graph.getVajramDefinition(depVajramID);
    Collection<VajramID> depVajramIDs = new ArrayList<>();
    if (depVajramDef.isTrait()) {
      TraitDispatchPolicy traitDispatchPolicy = traitDispatchPolicies.get(depVajramID);
      if (traitDispatchPolicy == null) {
        throw new IllegalStateException(
            "Trait "
                + depVajramID
                + " does not have a trait dispatch policy defined. Cannot auto-compute batcher config.");
      }
      for (VajramID vajramID : traitDispatchPolicy.dispatchTargetIDs()) {
        depVajramIDs.addAll(getDispatchTargets(vajramID, graph, traitDispatchPolicies));
      }
    } else {
      depVajramIDs = List.of(depVajramID);
    }
    return depVajramIDs;
  }

  private static int computeSourceOrdinal(
      Dependency dependency,
      int incomingOrdinal,
      Map<Dependency, Integer> dependencySourceOrdinals,
      Map<VajramID, Integer> vajramsToResponseOrdinals,
      VajramGraph graph,
      TraitDispatchPolicies traitDispatchPolicies) {
    if (dependencySourceOrdinals.containsKey(dependency)) {
      return dependencySourceOrdinals.get(dependency);
    }
    VajramDefinition vajramBeingInvoked = graph.getVajramDefinition(dependency.ofVajramID());
    int sourceOrdinal = incomingOrdinal;
    List<ResolverDefinition> resolvers =
        vajramBeingInvoked.inputResolvers().keySet().stream()
            .filter(r -> r.target().dependency().equals(dependency))
            .toList();
    for (ResolverDefinition resolver : resolvers) {
      ImmutableSet<? extends Facet> sources = resolver.sources();
      for (Facet source : sources) {
        if (source instanceof Dependency depSource) {
          for (VajramID sourceDispatchTarget :
              getDispatchTargets(depSource.onVajramID(), graph, traitDispatchPolicies)) {
            sourceOrdinal =
                max(
                    sourceOrdinal,
                    computeSourceOrdinal(
                            depSource,
                            incomingOrdinal,
                            dependencySourceOrdinals,
                            vajramsToResponseOrdinals,
                            graph,
                            traitDispatchPolicies)
                        + computeResponseOrdinal(
                            sourceDispatchTarget,
                            vajramsToResponseOrdinals,
                            graph,
                            traitDispatchPolicies));
          }
        }
      }
    }
    dependencySourceOrdinals.put(dependency, sourceOrdinal);
    return sourceOrdinal;
  }

  private static int computeResponseOrdinal(
      VajramID vajramBeingInvokedID,
      Map<VajramID, Integer> vajramsToResponseOrdinals,
      VajramGraph graph,
      TraitDispatchPolicies traitDispatchPolicies) {
    if (!vajramsToResponseOrdinals.containsKey(vajramBeingInvokedID)) {
      VajramDefinition vajramBeingInvoked = graph.getVajramDefinition(vajramBeingInvokedID);
      List<Dependency> dependencies = getDependencies(vajramBeingInvoked);
      Map<Dependency, List<ResolverDefinition>> resolversByTargetDep =
          vajramBeingInvoked.inputResolvers().keySet().stream()
              .collect(groupingBy(r -> r.target().dependency()));
      int sourceOrdinal = 0;
      for (Dependency dependency : dependencies) {
        List<ResolverDefinition> resolvers =
            resolversByTargetDep.getOrDefault(dependency, List.of());
        for (ResolverDefinition resolver : resolvers) {
          ImmutableSet<? extends Facet> sources = resolver.sources();
          for (Facet source : sources) {
            if (source instanceof Dependency depSource) {
              Collection<VajramID> dispatchTargets =
                  getDispatchTargets(depSource.onVajramID(), graph, traitDispatchPolicies);
              for (VajramID dispatchTarget : dispatchTargets) {
                sourceOrdinal =
                    max(
                        sourceOrdinal,
                        computeResponseOrdinal(
                            dispatchTarget,
                            vajramsToResponseOrdinals,
                            graph,
                            traitDispatchPolicies));
              }
            }
          }
        }
      }
      if (vajramBeingInvoked.def() instanceof IOVajramDef<Object>) {
        sourceOrdinal++;
      }
      vajramsToResponseOrdinals.put(vajramBeingInvokedID, sourceOrdinal);
    }
    return vajramsToResponseOrdinals.get(vajramBeingInvokedID);
  }
}
