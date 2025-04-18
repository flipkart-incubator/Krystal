package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.kryon.DefaultDependentChain;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.DependentChainStart;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig.LogicDecoratorContext;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.annos.VajramIdentifier;
import com.flipkart.krystal.vajram.batching.InputBatcher;
import com.flipkart.krystal.vajram.batching.InputBatcherImpl;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

public record InputBatcherConfig(
    Function<LogicExecutionContext, String> instanceIdGenerator,
    Predicate<BatcherContext> shouldBatch,
    Function<BatcherContext, OutputLogicDecorator> decoratorFactory) {

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
  public static InputBatcherConfig simple(Supplier<InputBatcher> inputBatcherSupplier) {
    return new InputBatcherConfig(
        logicExecutionContext ->
            generateInstanceId(
                    logicExecutionContext.dependants(),
                    logicExecutionContext.kryonDefinitionRegistry())
                .toString(),
        batcherContext -> true,
        batcherContext -> {
          return new InputBatchingDecorator(
              batcherContext.logicDecoratorContext().instanceId(),
              inputBatcherSupplier.get(),
              dependantChain ->
                  batcherContext
                      .logicDecoratorContext()
                      .logicExecutionContext()
                      .dependants()
                      .equals(dependantChain));
        });
  }

  public static InputBatcherConfig sharedBatcher(
      Supplier<InputBatcher> inputBatcherSupplier,
      String instanceId,
      DependentChain... dependentChains) {
    return sharedBatcher(inputBatcherSupplier, instanceId, ImmutableSet.copyOf(dependentChains));
  }

  public static InputBatcherConfig sharedBatcher(
      Supplier<InputBatcher> inputBatcherSupplier,
      String instanceId,
      ImmutableSet<DependentChain> dependentChains) {
    return new InputBatcherConfig(
        logicExecutionContext -> instanceId,
        batcherContext ->
            dependentChains.contains(
                batcherContext.logicDecoratorContext().logicExecutionContext().dependants()),
        batcherContext -> {
          return new InputBatchingDecorator(
              instanceId, inputBatcherSupplier.get(), dependentChains::contains);
        });
  }

  public static void autoRegisterSharedBatchers(
      VajramKryonGraph graph, BatchSizeSupplier batchSizeSupplier) {
    autoRegisterSharedBatchers(graph, batchSizeSupplier, ImmutableSet.of());
  }

  public static void autoRegisterSharedBatchers(
      VajramKryonGraph graph,
      BatchSizeSupplier batchSizeSupplier,
      ImmutableSet<DependentChain> disabledDependentChains) {
    Map<com.flipkart.krystal.core.VajramID, Map<Integer, Set<DependentChain>>> ioNodes =
        getIoVajrams(graph, disabledDependentChains);
    ioNodes.forEach(
        (vajramId, ioNodeMap) -> {
          int inputModulatorIndex = 0;
          if (isBatchingNeededForIoVajram(graph, vajramId)) {
            InputBatcherConfig[] inputModulatorConfigs = new InputBatcherConfig[ioNodeMap.size()];
            for (Entry<Integer, Set<DependentChain>> entry : ioNodeMap.entrySet()) {
              Set<DependentChain> depChains = entry.getValue();
              inputModulatorConfigs[inputModulatorIndex++] =
                  InputBatcherConfig.sharedBatcher(
                      () -> new InputBatcherImpl(batchSizeSupplier.getBatchSize(vajramId)),
                      vajramId.id(),
                      depChains.toArray(DependentChain[]::new));
            }
            graph.registerInputBatchers(vajramId, inputModulatorConfigs);
          }
        });
  }

  /**
   * @return decorator instanceId of the form {@code
   *     [Start]>vajramId_1:dep_1>vajramId_2:dep_2>....>vajramId_n:dep_n}
   */
  private static StringBuilder generateInstanceId(
      DependentChain dependentChain, KryonDefinitionRegistry kryonDefinitionRegistry) {
    if (dependentChain instanceof DependentChainStart dependantChainStart) {
      return new StringBuilder(dependantChainStart.toString());
    } else if (dependentChain instanceof DefaultDependentChain defaultDependantChain) {
      if (defaultDependantChain.incomingDependentChain() instanceof DependentChainStart) {
        Optional<VajramIdentifier> vajramIdAnno =
            kryonDefinitionRegistry
                .getOrThrow(defaultDependantChain.kryonId())
                .tags()
                .getAnnotationByType(VajramIdentifier.class);
        if (vajramIdAnno.isPresent()) {
          return generateInstanceId(
                  defaultDependantChain.incomingDependentChain(), kryonDefinitionRegistry)
              .append('>')
              .append(vajramIdAnno.get().value())
              .append(':')
              .append(defaultDependantChain.latestDependency());
        } else {
          throw new NoSuchElementException(
              "Could not find tag %s for kryon %s"
                  .formatted(Vajram.class, defaultDependantChain.kryonId()));
        }
      } else {
        return generateInstanceId(
                defaultDependantChain.incomingDependentChain(), kryonDefinitionRegistry)
            .append('>')
            .append(defaultDependantChain.latestDependency());
      }
    }
    throw new UnsupportedOperationException();
  }

  private static boolean isBatchingNeededForIoVajram(
      VajramKryonGraph graph, com.flipkart.krystal.core.VajramID ioNode) {
    VajramDefinition ioNodeVajram = graph.getVajramDefinition(ioNode);
    for (FacetSpec facetSpec : ioNodeVajram.facetSpecs()) {
      if (facetSpec.isBatched()) {
        return true;
      }
    }
    return false;
  }

  private static Map<com.flipkart.krystal.core.VajramID, Map<Integer, Set<DependentChain>>>
      getIoVajrams(VajramKryonGraph graph, ImmutableSet<DependentChain> disabledDependentChains) {
    Map<com.flipkart.krystal.core.VajramID, Map<Integer, Set<DependentChain>>> ioNodes =
        new HashMap<>();
    for (VajramDefinition rootNode : externallyInvocableVajrams(graph)) {
      DependentChain dependentChain = graph.kryonDefinitionRegistry().getDependantChainsStart();
      Map<com.flipkart.krystal.core.VajramID, Integer> ioNodeDepths = new HashMap<>();
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
      Map<com.flipkart.krystal.core.VajramID, Map<Integer, Set<DependentChain>>> ioNodes,
      int depth,
      DependentChain incomingDepChain,
      Map<com.flipkart.krystal.core.VajramID, Integer> ioNodeDepths,
      ImmutableSet<DependentChain> disabledDependentChains) {
    // find all the child nodes of rootNode
    // get the order of execution of inputDefinitions
    Map<Facet, List<Facet>> inputDefGraph = new HashMap<>();
    VajramID vajramId = rootNode.vajramId();
    graph.loadKryonSubGraphIfNeeded(vajramId);
    for (Facet inputDef : getOrderedInputDef(rootNode, inputDefGraph)) {
      if (inputDef instanceof DependencySpec<?, ?, ?> dependency) {
        List<ResolverDefinition> resolverDefinition =
            getInputResolverDefinition(rootNode, dependency);
        VajramDefinition childNode = graph.getVajramDefinition(dependency.onVajramId());
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
      VajramDefinition node,
      VajramKryonGraph graph,
      Map<com.flipkart.krystal.core.VajramID, Integer> ioNodeDepth) {
    if (node.def() instanceof IOVajramDef<?>) {
      ioNodeDepth.compute(node.vajramId(), (_vid, depth) -> depth == null ? 0 : depth + 1);
    }
    for (Facet inputDef : node.facetSpecs()) {
      if (inputDef instanceof DependencySpec<?, ?, ?> dependency) {
        incrementTheLeafIONodeOfTheVajram(
            graph.getVajramDefinition(dependency.onVajramId()), graph, ioNodeDepth);
      }
    }
  }

  private static void decrementTheLeafIONodeOfTheVajram(
      VajramDefinition node,
      VajramKryonGraph graph,
      Map<com.flipkart.krystal.core.VajramID, Integer> ioNodeDepth) {
    if (node.def() instanceof IOVajramDef<?>) {
      ioNodeDepth.compute(node.vajramId(), (_vid, depth) -> depth == null ? 0 : depth - 1);
    }
    for (Facet inputDef : node.facetSpecs()) {
      if (inputDef instanceof DependencySpec<?, ?, ?> dependency) {
        decrementTheLeafIONodeOfTheVajram(
            graph.getVajramDefinition(dependency.onVajramId()), graph, ioNodeDepth);
      }
    }
  }

  private static @Nullable VajramID dependencyInputInChildNode(
      List<ResolverDefinition> depInputs, Facet inputDefinition) {
    for (ResolverDefinition depInput : depInputs) {
      if (inputDefinition instanceof DependencySpec<?, ?, ?> dependency) {
        if (depInput.sources().contains(inputDefinition)) {
          return dependency.onVajramId();
        }
      }
    }
    return null;
  }

  private static List<ResolverDefinition> getInputResolverDefinition(
      VajramDefinition rootNode, DependencySpec<?, ?, ?> dependency) {
    return rootNode.inputResolvers().values().stream()
        .filter(
            inputResolver ->
                inputResolver.definition().target().dependency().id() == dependency.id())
        .map(InputResolver::definition)
        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
  }

  private static Collection<Facet> getOrderedInputDef(
      VajramDefinition rootNode, Map<Facet, List<Facet>> graph) {

    ImmutableCollection<InputResolver> resolvers = rootNode.inputResolvers().values();
    ImmutableCollection<FacetSpec> inputDefinitions = rootNode.facetSpecs();
    for (InputResolver resolver : resolvers) {
      ResolverDefinition resolverDefinition = resolver.definition();
      for (Facet facet : inputDefinitions) {
        if (facet.facetTypes().contains(DEPENDENCY)) {
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
      if (vid.facetTypes().contains(DEPENDENCY)) {
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
      if (facet.facetTypes().contains(DEPENDENCY)) {
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
    if (vid.facetTypes().contains(DEPENDENCY)) {
      stack.add(vid);
    }
  }

  public record BatcherContext(LogicDecoratorContext logicDecoratorContext) {}

  @FunctionalInterface
  public interface BatchSizeSupplier {
    int getBatchSize(com.flipkart.krystal.core.VajramID vajramId);
  }
}
