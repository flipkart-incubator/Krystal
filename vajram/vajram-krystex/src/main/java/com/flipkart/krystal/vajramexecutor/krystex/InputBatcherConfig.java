package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.kryon.DefaultDependantChain;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.DependantChainStart;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig.LogicDecoratorContext;
import com.flipkart.krystal.vajram.BatchableVajram;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.batching.BatchableSupplier;
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
   * Creates a default InputBatcherConfig which guarantees that every unique {@link DependantChain}
   * of a vajram gets its own {@link InputBatchingDecorator} and its own corresponding {@link
   * InputBatcher}. The instance id corresponding to a particular {@link DependantChain} is of the
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
          @SuppressWarnings("unchecked")
          BatchableSupplier<Facets, Facets> facetsConvertor =
              (BatchableSupplier<Facets, Facets>) batcherContext.vajram().getBatchFacetsConvertor();
          return new InputBatchingDecorator(
              batcherContext.logicDecoratorContext().instanceId(),
              inputBatcherSupplier.get(),
              facetsConvertor,
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
      DependantChain... dependantChains) {
    return sharedBatcher(inputBatcherSupplier, instanceId, ImmutableSet.copyOf(dependantChains));
  }

  public static InputBatcherConfig sharedBatcher(
      Supplier<InputBatcher> inputBatcherSupplier,
      String instanceId,
      ImmutableSet<DependantChain> dependantChains) {
    return new InputBatcherConfig(
        logicExecutionContext -> instanceId,
        batcherContext ->
            dependantChains.contains(
                batcherContext.logicDecoratorContext().logicExecutionContext().dependants()),
        batcherContext -> {
          @SuppressWarnings("unchecked")
          BatchableSupplier<Facets, Facets> facetsConvertor =
              (BatchableSupplier<Facets, Facets>) batcherContext.vajram().getBatchFacetsConvertor();
          return new InputBatchingDecorator(
              instanceId, inputBatcherSupplier.get(), facetsConvertor, dependantChains::contains);
        });
  }

  public static void autoRegisterSharedBatchers(
      VajramKryonGraph graph, BatchSizeSupplier batchSizeSupplier) {
    autoRegisterSharedBatchers(graph, batchSizeSupplier, ImmutableSet.of());
  }

  public static void autoRegisterSharedBatchers(
      VajramKryonGraph graph,
      BatchSizeSupplier batchSizeSupplier,
      ImmutableSet<DependantChain> disabledDependantChains) {
    Map<VajramID, Map<Integer, Set<DependantChain>>> ioNodes =
        getIoVajrams(graph, disabledDependantChains);
    ioNodes.forEach(
        (vajramId, ioNodeMap) -> {
          int inputModulatorIndex = 0;
          if (isBatchingNeededForIoVajram(graph, vajramId)) {
            InputBatcherConfig[] inputModulatorConfigs = new InputBatcherConfig[ioNodeMap.size()];
            for (Entry<Integer, Set<DependantChain>> entry : ioNodeMap.entrySet()) {
              Set<DependantChain> depChains = entry.getValue();
              inputModulatorConfigs[inputModulatorIndex++] =
                  InputBatcherConfig.sharedBatcher(
                      () -> new InputBatcherImpl(batchSizeSupplier.getBatchSize(vajramId)),
                      vajramId.vajramId(),
                      depChains.toArray(DependantChain[]::new));
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
      DependantChain dependantChain, KryonDefinitionRegistry kryonDefinitionRegistry) {
    if (dependantChain instanceof DependantChainStart dependantChainStart) {
      return new StringBuilder(dependantChainStart.toString());
    } else if (dependantChain instanceof DefaultDependantChain defaultDependantChain) {
      if (defaultDependantChain.dependantChain() instanceof DependantChainStart) {
        Optional<VajramDef> vajramDef =
            kryonDefinitionRegistry
                .get(defaultDependantChain.kryonId())
                .tags()
                .getAnnotationByType(VajramDef.class);
        if (vajramDef.isPresent()) {
          return generateInstanceId(defaultDependantChain.dependantChain(), kryonDefinitionRegistry)
              .append('>')
              .append(vajramDef.get().id())
              .append(':')
              .append(defaultDependantChain.dependency());
        } else {
          throw new NoSuchElementException(
              "Could not find tag %s for kryon %s"
                  .formatted(VajramDef.class, defaultDependantChain.kryonId()));
        }
      } else {
        return generateInstanceId(defaultDependantChain.dependantChain(), kryonDefinitionRegistry)
            .append('>')
            .append(defaultDependantChain.dependency());
      }
    }
    throw new UnsupportedOperationException();
  }

  private static boolean isBatchingNeededForIoVajram(VajramKryonGraph graph, VajramID ioNode) {
    Optional<VajramDefinition> ioNodeVajram = graph.getVajramDefinition(ioNode);
    if (ioNodeVajram.isPresent()) {
      for (FacetSpec facetSpec : ioNodeVajram.get().facetSpecs()) {
        if (facetSpec.isBatched()) {
          return true;
        }
      }
    }
    return false;
  }

  private static Map<VajramID, Map<Integer, Set<DependantChain>>> getIoVajrams(
      VajramKryonGraph graph, ImmutableSet<DependantChain> disabledDependantChains) {
    Map<VajramID, Map<Integer, Set<DependantChain>>> ioNodes = new HashMap<>();
    for (VajramDefinition rootNode : externallyInvocableVajrams(graph)) {
      DependantChain dependantChain = graph.kryonDefinitionRegistry().getDependantChainsStart();
      Map<VajramID, Integer> ioNodeDepths = new HashMap<>();
      dfs(rootNode, graph, ioNodes, 0, dependantChain, ioNodeDepths, disabledDependantChains);
    }
    return ioNodes;
  }

  private static Iterable<VajramDefinition> externallyInvocableVajrams(VajramKryonGraph graph) {
    return graph.vajramDefinitions().values().stream()
        .filter(
            v -> {
              return v.vajramTags()
                  .getAnnotationByType(ExternalInvocation.class)
                  .map(ExternalInvocation::allow)
                  .orElse(false);
            })
        .toList();
  }

  private static void dfs(
      VajramDefinition rootNode,
      VajramKryonGraph graph,
      Map<VajramID, Map<Integer, Set<DependantChain>>> ioNodes,
      int depth,
      DependantChain incomingDepChain,
      Map<VajramID, Integer> ioNodeDepths,
      ImmutableSet<DependantChain> disabledDependantChains) {
    // find all the child nodes of rootNode
    // get the order of execution of inputDefinitions
    Map<Facet, List<Facet>> inputDefGraph = new HashMap<>();
    KryonId kryonId = graph.getKryonId(rootNode.vajramId());
    for (Facet inputDef : getOrderedInputDef(rootNode, inputDefGraph)) {
      if (inputDef instanceof DependencySpec<?, ?, ?> dependency) {
        List<ResolverDefinition> resolverDefinition =
            getInputResolverDefinition(rootNode, dependency);
        VajramDefinition childNode =
            graph.getVajramDefinition(dependency.onVajramId()).orElse(null);
        if (childNode != null) {
          if (inputDefGraph.get(inputDef) != null) {
            for (Facet inputDef1 : inputDefGraph.get(inputDef)) {
              VajramID prerequisiteVajramId =
                  dependencyInputInChildNode(resolverDefinition, inputDef1);
              if (prerequisiteVajramId != null) {
                graph
                    .getVajramDefinition(prerequisiteVajramId)
                    .ifPresent(
                        vajramDefinition ->
                            incrementTheLeafIONodeOfTheVajram(
                                vajramDefinition, graph, ioNodeDepths));
              }
            }
          }
          DependantChain dependantChain = incomingDepChain.extend(kryonId, dependency);
          if (!disabledDependantChains.contains(dependantChain)) {
            if (childNode.vajram() instanceof IOVajram<?>) {
              depth = ioNodeDepths.computeIfAbsent(childNode.vajramId(), _v -> 0);
              ioNodes
                  .computeIfAbsent(childNode.vajramId(), k -> new HashMap<>())
                  .computeIfAbsent(depth, k -> new LinkedHashSet<>())
                  .add(dependantChain);
            }
            dfs(
                childNode,
                graph,
                ioNodes,
                depth,
                dependantChain,
                ioNodeDepths,
                disabledDependantChains);
            if (inputDefGraph.get(inputDef) != null) {
              for (Facet inputDef1 : inputDefGraph.get(inputDef)) {
                VajramID prerequisiteVajramId =
                    dependencyInputInChildNode(resolverDefinition, inputDef1);
                if (prerequisiteVajramId != null
                    && graph.getVajramDefinition(prerequisiteVajramId).isPresent()) {
                  decrementTheLeafIONodeOfTheVajram(
                      graph.getVajramDefinition(prerequisiteVajramId).get(), graph, ioNodeDepths);
                }
              }
            }
          }
        }
      }
    }
  }

  private static void incrementTheLeafIONodeOfTheVajram(
      VajramDefinition node, VajramKryonGraph graph, Map<VajramID, Integer> ioNodeDepth) {
    if (node.vajram() instanceof IOVajram<?>) {
      ioNodeDepth.compute(node.vajramId(), (_vid, depth) -> depth == null ? 0 : depth + 1);
    }
    for (Facet inputDef : node.facetSpecs()) {
      if (inputDef instanceof DependencySpec<?, ?, ?> dependency) {
        Optional<VajramDefinition> depVajram = graph.getVajramDefinition(dependency.onVajramId());
        depVajram.ifPresent(
            vajramDefinition ->
                incrementTheLeafIONodeOfTheVajram(vajramDefinition, graph, ioNodeDepth));
      }
    }
  }

  private static void decrementTheLeafIONodeOfTheVajram(
      VajramDefinition node, VajramKryonGraph graph, Map<VajramID, Integer> ioNodeDepth) {
    if (node.vajram() instanceof IOVajram<?>) {
      ioNodeDepth.compute(node.vajramId(), (_vid, depth) -> depth == null ? 0 : depth - 1);
    }
    for (Facet inputDef : node.facetSpecs()) {
      if (inputDef instanceof DependencySpec<?, ?, ?> dependency) {
        Optional<VajramDefinition> depVajram = graph.getVajramDefinition(dependency.onVajramId());
        depVajram.ifPresent(
            childNode -> decrementTheLeafIONodeOfTheVajram(childNode, graph, ioNodeDepth));
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

  public record BatcherContext(
      BatchableVajram<?> vajram, LogicDecoratorContext logicDecoratorContext) {}

  @FunctionalInterface
  public interface BatchSizeSupplier {
    int getBatchSize(VajramID vajramId);
  }
}
