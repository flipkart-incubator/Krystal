package com.flipkart.krystal.vajramexecutor.krystex.batching;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;
import static java.lang.Math.max;
import static java.util.stream.Collectors.groupingBy;

import com.flipkart.krystal.annos.ExternallyInvocable;
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
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.batching.InputBatcher;
import com.flipkart.krystal.vajram.batching.InputBatcherImpl;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
        logicExecutionContext -> generateInstanceId(logicExecutionContext.dependants()).toString(),
        outputLogicDecoratorContext ->
            new InputBatchingDecorator(
                outputLogicDecoratorContext.instanceId(),
                inputBatcherSupplier.get(),
                dependantChain ->
                    outputLogicDecoratorContext
                        .logicExecutionContext()
                        .dependants()
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
        logicExecutionContext -> dependentChains.contains(logicExecutionContext.dependants()),
        logicExecutionContext -> instanceId,
        outputLogicDecoratorContext ->
            new InputBatchingDecorator(
                instanceId, inputBatcherSupplier.get(), dependentChains::contains));
  }

  @Deprecated(forRemoval = true)
  public static void autoRegisterSharedBatchers(
      VajramKryonGraph graph, BatchSizeSupplier batchSizeSupplier) {
    autoRegisterSharedBatchers(graph, batchSizeSupplier, ImmutableSet.of());
  }

  @Deprecated(forRemoval = true)
  public static void autoRegisterSharedBatchers(
      VajramKryonGraph graph,
      BatchSizeSupplier batchSizeSupplier,
      ImmutableSet<DependentChain> disabledDependentChains) {
    Map<VajramID, Map<Integer, Set<DependentChain>>> ioNodes =
        getIoVajrams(graph, disabledDependentChains);
    registerBatchers(ioNodes, batchSizeSupplier, graph);
  }

  private static void registerBatchers(
      Map<VajramID, Map<Integer, Set<DependentChain>>> ioNodes,
      BatchSizeSupplier batchSizeSupplier,
      VajramKryonGraph graph) {
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
    graph.registerInputBatchers(
        new InputBatcherConfig(ImmutableMap.copyOf(depChainBatcherConfigs)));
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

  private static boolean isBatchingNeededForIoVajram(VajramKryonGraph graph, VajramID ioNode) {
    VajramDefinition ioNodeVajram = graph.getVajramDefinition(ioNode);
    for (FacetSpec facetSpec : ioNodeVajram.facetSpecs()) {
      if (facetSpec.isBatched()) {
        return true;
      }
    }
    return false;
  }

  private static Map<VajramID, Map<Integer, Set<DependentChain>>> getIoVajrams(
      VajramKryonGraph graph, ImmutableSet<DependentChain> disabledDependentChains) {
    Map<VajramID, Map<Integer, Set<DependentChain>>> ioNodes = new HashMap<>();
    for (VajramDefinition rootNode : externallyInvocableVajrams(graph)) {
      DependentChain dependentChain = graph.kryonDefinitionRegistry().getDependentChainsStart();
      Map<VajramID, Integer> ioNodeDepths = new HashMap<>();
      dfs(rootNode, graph, ioNodes, 0, dependentChain, ioNodeDepths, disabledDependentChains);
    }
    return ioNodes;
  }

  private static Iterable<VajramDefinition> externallyInvocableVajrams(VajramKryonGraph graph) {
    return graph.vajramDefinitions().values().stream()
        .filter(v -> v.vajramTags().getAnnotationByType(ExternallyInvocable.class).isPresent())
        .toList();
  }

  private static void dfs(
      VajramDefinition rootNode,
      VajramKryonGraph graph,
      Map<VajramID, Map<Integer, Set<DependentChain>>> ioNodes,
      int depth,
      DependentChain incomingDepChain,
      Map<VajramID, Integer> ioNodeDepths,
      ImmutableSet<DependentChain> disabledDependentChains) {
    // find all the child nodes of rootNode
    // get the order of execution of inputDefinitions
    Map<Facet, List<Facet>> inputDefGraph = new HashMap<>();
    VajramID vajramId = rootNode.vajramId();
    for (Facet inputDef : getOrderedInputDef(rootNode, inputDefGraph)) {
      if (inputDef instanceof DependencySpec<?, ?, ?> dependency) {
        List<ResolverDefinition> resolverDefinition =
            getInputResolverDefinition(rootNode, dependency);
        VajramDefinition childNode = graph.getVajramDefinition(dependency.onVajramID());
        if (inputDefGraph.get(inputDef) != null) {
          for (Facet inputDef1 : inputDefGraph.get(inputDef)) {
            VajramID prerequisiteVajramId =
                dependencyInputInChildNode(resolverDefinition, inputDef1);
            if (prerequisiteVajramId != null) {
              incrementTheLeafIONodeOfTheVajram(
                  graph.getVajramDefinition(prerequisiteVajramId), graph, ioNodeDepths);
            }
          }
        }
        DependentChain dependentChain = incomingDepChain.extend(vajramId, dependency);
        if (!disabledDependentChains.contains(dependentChain)) {
          if (childNode.def() instanceof IOVajramDef<?>) {
            depth = ioNodeDepths.computeIfAbsent(childNode.vajramId(), _v -> 0);
            ioNodes
                .computeIfAbsent(childNode.vajramId(), k -> new HashMap<>())
                .computeIfAbsent(depth, k -> new LinkedHashSet<>())
                .add(dependentChain);
          }
          dfs(
              childNode,
              graph,
              ioNodes,
              depth,
              dependentChain,
              ioNodeDepths,
              disabledDependentChains);
          if (inputDefGraph.get(inputDef) != null) {
            for (Facet inputDef1 : inputDefGraph.get(inputDef)) {
              VajramID prerequisiteVajramId =
                  dependencyInputInChildNode(resolverDefinition, inputDef1);
              if (prerequisiteVajramId != null) {
                graph.getVajramDefinition(prerequisiteVajramId);
                decrementTheLeafIONodeOfTheVajram(
                    graph.getVajramDefinition(prerequisiteVajramId), graph, ioNodeDepths);
              }
            }
          }
        }
      }
    }
  }

  private static void incrementTheLeafIONodeOfTheVajram(
      VajramDefinition node, VajramKryonGraph graph, Map<VajramID, Integer> ioNodeDepth) {
    if (node.def() instanceof IOVajramDef<?>) {
      ioNodeDepth.compute(node.vajramId(), (_vid, depth) -> depth == null ? 0 : depth + 1);
    }
    for (Facet inputDef : node.facetSpecs()) {
      if (inputDef instanceof DependencySpec<?, ?, ?> dependency) {
        incrementTheLeafIONodeOfTheVajram(
            graph.getVajramDefinition(dependency.onVajramID()), graph, ioNodeDepth);
      }
    }
  }

  private static void decrementTheLeafIONodeOfTheVajram(
      VajramDefinition node, VajramKryonGraph graph, Map<VajramID, Integer> ioNodeDepth) {
    if (node.def() instanceof IOVajramDef<?>) {
      ioNodeDepth.compute(node.vajramId(), (_vid, depth) -> depth == null ? 0 : depth - 1);
    }
    for (Facet inputDef : node.facetSpecs()) {
      if (inputDef instanceof DependencySpec<?, ?, ?> dependency) {
        decrementTheLeafIONodeOfTheVajram(
            graph.getVajramDefinition(dependency.onVajramID()), graph, ioNodeDepth);
      }
    }
  }

  private static @Nullable VajramID dependencyInputInChildNode(
      List<ResolverDefinition> depInputs, Facet inputDefinition) {
    for (ResolverDefinition depInput : depInputs) {
      if (inputDefinition instanceof DependencySpec<?, ?, ?> dependency) {
        if (depInput.sources().contains(inputDefinition)) {
          return dependency.onVajramID();
        }
      }
    }
    return null;
  }

  private static List<ResolverDefinition> getInputResolverDefinition(
      VajramDefinition rootNode, DependencySpec<?, ?, ?> dependency) {
    return rootNode.inputResolvers().values().stream()
        .map(InputResolver::definition)
        .filter(definition -> definition.target().dependency().id() == dependency.id())
        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
  }

  private static Collection<Facet> getOrderedInputDef(
      VajramDefinition rootNode, Map<Facet, List<Facet>> graph) {

    ImmutableCollection<InputResolver> resolvers = rootNode.inputResolvers().values();
    ImmutableCollection<FacetSpec> inputDefinitions = rootNode.facetSpecs();
    for (InputResolver resolver : resolvers) {
      ResolverDefinition resolverDefinition = resolver.definition();
      for (Facet facet : inputDefinitions) {
        if (DEPENDENCY.equals(facet.facetType())) {
          if (resolverDefinition.sources().contains(facet)) {
            Facet dependingVID =
                getInputDefinitionDep(resolverDefinition.target().dependency(), inputDefinitions);
            if (dependingVID != null) {
              graph.putIfAbsent(dependingVID, new ArrayList<>());
              graph.get(dependingVID).add(facet);
            }
          }
        }
      }
    }
    Set<Facet> visited = new HashSet<>();
    Queue<Facet> queue = new ArrayDeque<>();
    for (Facet vid : inputDefinitions) {
      if (DEPENDENCY.equals(vid.facetType())) {
        if (!visited.contains(vid)) {
          topologicalSortUtil(vid, visited, graph, queue);
        }
      }
    }
    return queue;
  }

  private static @Nullable Facet getInputDefinitionDep(
      Facet dep, ImmutableCollection<? extends Facet> inputDefinitions) {
    for (Facet facet : inputDefinitions) {
      if (DEPENDENCY.equals(facet.facetType())) {
        if (facet.id() == dep.id()) {
          return facet;
        }
      }
    }
    return null;
  }

  static void topologicalSortUtil(
      Facet vid, Set<Facet> visited, Map<Facet, List<Facet>> graph, Queue<Facet> stack) {
    visited.add(vid);
    for (Facet i : graph.getOrDefault(vid, new ArrayList<>())) {
      if (!visited.contains(i)) {
        topologicalSortUtil(i, visited, graph, stack);
      }
    }
    if (vid.facetType().equals(DEPENDENCY)) {
      stack.add(vid);
    }
  }

  @FunctionalInterface
  public interface BatchSizeSupplier {
    int getBatchSize(VajramID vajramId);
  }

  public static void autoRegisterSharedBatchersV2(
      VajramKryonGraph graph, BatchSizeSupplier batchSizeSupplier) {
    autoRegisterSharedBatchersV2(graph, batchSizeSupplier, ImmutableSet.of());
  }

  public static void autoRegisterSharedBatchersV2(
      VajramKryonGraph graph,
      BatchSizeSupplier batchSizeSupplier,
      ImmutableSet<DependentChain> disabledDependentChains) {
    List<VajramDefinition> externallyInvocableVajrams =
        graph.vajramDefinitions().values().stream()
            .filter(v -> v.vajramTags().getAnnotationByType(ExternallyInvocable.class).isPresent())
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
          disabledDependentChains);
    }
    registerBatchers(ioVajramsToOrdinalChains, batchSizeSupplier, graph);
  }

  private static void collateDepChainOrdinals(
      Map<VajramID, Map<Integer, Set<DependentChain>>> ioVajramsToOrdinalChains,
      Map<VajramID, Integer> vajramsToResponseOrdinals,
      VajramKryonGraph graph,
      VajramID vajramIDBeingInvoked,
      DependentChain incomingDepChain,
      int incomingOrdinal,
      ImmutableSet<DependentChain> disabledDependentChains) {
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
          // TODO: Replace (DependencySpec) casting with accessing onVajramID
          //  directly from Dependency
          getDispatchTargets(((DependencySpec) dependency).onVajramID(), graph)) {
        int depSourceOrdinal =
            computeSourceOrdinal(
                dependency, incomingOrdinal, dependencyOrdinals, vajramsToResponseOrdinals, graph);
        collateDepChainOrdinals(
            ioVajramsToOrdinalChains,
            vajramsToResponseOrdinals,
            graph,
            depVajramID,
            outgoingDepChain,
            depSourceOrdinal,
            disabledDependentChains);
      }
    }

    if (vajramBeingInvoked.def() instanceof IOVajramDef<?>) {
      final int depChainOrdinal =
          incomingOrdinal
              + computeResponseOrdinal(vajramIDBeingInvoked, vajramsToResponseOrdinals, graph);
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
      VajramID depVajramID, VajramKryonGraph graph) {
    VajramDefinition depVajramDef = graph.getVajramDefinition(depVajramID);
    Collection<VajramID> depVajramIDs = new ArrayList<>();
    if (depVajramDef.isTrait()) {
      TraitDispatchPolicy traitDispatchPolicy = graph.getTraitDispatchPolicy(depVajramID);
      if (traitDispatchPolicy == null) {
        throw new IllegalStateException(
            "Trait "
                + depVajramID
                + " does not have a trait dispatch policy defined. Cannot auto-compute batcher config.");
      }
      for (VajramID vajramID : traitDispatchPolicy.dispatchTargets()) {
        depVajramIDs.addAll(getDispatchTargets(vajramID, graph));
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
      VajramKryonGraph graph) {
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
              // TODO: Replace (DependencySpec) casting with accessing onVajramID
              //  directly from Dependency
              getDispatchTargets(((DependencySpec) depSource).onVajramID(), graph)) {
            sourceOrdinal =
                max(
                    sourceOrdinal,
                    computeSourceOrdinal(
                            depSource,
                            incomingOrdinal,
                            dependencySourceOrdinals,
                            vajramsToResponseOrdinals,
                            graph)
                        + computeResponseOrdinal(
                            sourceDispatchTarget, vajramsToResponseOrdinals, graph));
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
      VajramKryonGraph graph) {
    return vajramsToResponseOrdinals.computeIfAbsent(
        vajramBeingInvokedID,
        vajramID -> {
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
                  // TODO: Replace (DependencySpec) casting with accessing onVajramID
                  //  directly from Dependency
                  Collection<VajramID> dispatchTargets =
                      getDispatchTargets(((DependencySpec) depSource).onVajramID(), graph);
                  for (VajramID dispatchTarget : dispatchTargets) {
                    sourceOrdinal =
                        max(
                            sourceOrdinal,
                            computeResponseOrdinal(
                                dispatchTarget, vajramsToResponseOrdinals, graph));
                  }
                } else {
                  sourceOrdinal = max(sourceOrdinal, 0);
                }
              }
            }
          }
          if (vajramBeingInvoked.def() instanceof IOVajramDef<Object>) {
            sourceOrdinal++;
          }
          return sourceOrdinal;
        });
  }
}
