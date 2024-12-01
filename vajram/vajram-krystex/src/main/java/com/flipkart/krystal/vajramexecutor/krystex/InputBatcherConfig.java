package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.vajram.VajramID.vajramID;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.krystex.kryon.DefaultDependantChain;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.DependantChainStart;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig.LogicDecoratorContext;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.batching.FacetsConverter;
import com.flipkart.krystal.vajram.batching.InputBatcher;
import com.flipkart.krystal.vajram.batching.InputBatcherImpl;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.facets.DependencyDef;
import com.flipkart.krystal.vajram.facets.FacetValuesAdaptor;
import com.flipkart.krystal.vajram.facets.InputDef;
import com.flipkart.krystal.vajram.facets.VajramFacetDefinition;
import com.flipkart.krystal.vajram.facets.resolution.InputResolverDefinition;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

public record InputBatcherConfig(
    Function<LogicExecutionContext, String> instanceIdGenerator,
    Predicate<LogicExecutionContext> shouldBatch,
    Function<BatcherContext, OutputLogicDecorator> decoratorFactory) {

  private static final String DEP_DELIMITER = "<>";
  private static final Pattern DEP_DELIMITER_PATTERN = Pattern.compile(DEP_DELIMITER);

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
  public static InputBatcherConfig simple(
      Supplier<InputBatcher<FacetValuesAdaptor, FacetValuesAdaptor>> inputBatcherSupplier) {
    return new InputBatcherConfig(
        logicExecutionContext ->
            generateInstanceId(
                    logicExecutionContext.dependants(),
                    logicExecutionContext.kryonDefinitionRegistry())
                .toString(),
        _x -> true,
        batcherContext -> {
          @SuppressWarnings("unchecked")
          var inputsConvertor =
              (FacetsConverter<FacetValuesAdaptor, FacetValuesAdaptor>)
                  batcherContext.vajram().getInputsConvertor();
          return new InputBatchingDecorator<>(
              batcherContext.logicDecoratorContext().instanceId(),
              inputBatcherSupplier.get(),
              inputsConvertor,
              dependantChain ->
                  batcherContext
                      .logicDecoratorContext()
                      .logicExecutionContext()
                      .dependants()
                      .equals(dependantChain));
        });
  }

  public static InputBatcherConfig sharedBatcher(
      Supplier<InputBatcher<FacetValuesAdaptor, FacetValuesAdaptor>> inputBatcherSupplier,
      String instanceId,
      DependantChain... dependantChains) {
    return sharedBatcher(inputBatcherSupplier, instanceId, ImmutableSet.copyOf(dependantChains));
  }

  public static InputBatcherConfig sharedBatcher(
      Supplier<InputBatcher<FacetValuesAdaptor, FacetValuesAdaptor>> inputBatcherSupplier,
      String instanceId,
      ImmutableSet<DependantChain> dependantChains) {
    return new InputBatcherConfig(
        logicExecutionContext -> instanceId,
        logicExecutionContext -> dependantChains.contains(logicExecutionContext.dependants()),
        batcherContext -> {
          @SuppressWarnings("unchecked")
          var inputsConvertor =
              (FacetsConverter<FacetValuesAdaptor, FacetValuesAdaptor>)
                  batcherContext.vajram().getInputsConvertor();
          return new InputBatchingDecorator<>(
              instanceId, inputBatcherSupplier.get(), inputsConvertor, dependantChains::contains);
        });
  }

  public static void autoRegisterSharedBatchers(
      VajramKryonGraph graph, BatchSizeSupplier batchSizeSupplier) {
    Map<String, Map<Integer, List<String>>> ioNodes = getIoVajrams(graph);
    ioNodes.forEach(
        (vajramId, ioNodeMap) -> {
          int inputModulatorIndex = 0;
          if (isBatchingNeededForIoVajram(graph, vajramId)) {
            InputBatcherConfig[] inputModulatorConfigs = new InputBatcherConfig[ioNodeMap.size()];
            for (Map.Entry<Integer, List<String>> entry : ioNodeMap.entrySet()) {
              List<String> depChainList = entry.getValue();
              DependantChain[] dependantChainArray = new DependantChain[depChainList.size()];
              int depChainIndex = 0;
              for (String depChain : depChainList) {
                String[] kryonIdsArray = DEP_DELIMITER_PATTERN.split(depChain);
                dependantChainArray[depChainIndex++] =
                    graph.computeDependantChain(
                        kryonIdsArray[0],
                        kryonIdsArray[1],
                        Arrays.copyOfRange(kryonIdsArray, 2, kryonIdsArray.length));
              }
              inputModulatorConfigs[inputModulatorIndex++] =
                  InputBatcherConfig.sharedBatcher(
                      () -> new InputBatcherImpl<>(batchSizeSupplier.getBatchSize(vajramId)),
                      vajramId,
                      dependantChainArray);
            }
            graph.registerInputBatchers(vajramID(vajramId), inputModulatorConfigs);
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
              .append(defaultDependantChain.dependencyName());
        } else {
          throw new NoSuchElementException(
              "Could not find tag %s for kryon %s"
                  .formatted(VajramDef.class, defaultDependantChain.kryonId()));
        }
      } else {
        return generateInstanceId(defaultDependantChain.dependantChain(), kryonDefinitionRegistry)
            .append('>')
            .append(defaultDependantChain.dependencyName());
      }
    }
    throw new UnsupportedOperationException();
  }

  private static boolean isBatchingNeededForIoVajram(VajramKryonGraph graph, String ioNode) {
    Optional<VajramDefinition> ioNodeVajram = graph.getVajramDefinition(vajramID(ioNode));
    if (ioNodeVajram.isPresent()) {
      for (VajramFacetDefinition inputDefinition :
          ioNodeVajram.get().vajram().getFacetDefinitions()) {
        if (inputDefinition instanceof InputDef<?> inputDef && inputDef.isBatched()) {
          return true;
        }
      }
    }
    return false;
  }

  private static Map<String, Map<Integer, List<String>>> getIoVajrams(VajramKryonGraph graph) {
    Map<String, Map<Integer, List<String>>> ioNodes = new HashMap<>();
    for (VajramDefinition rootNode : externallyInvocableVajrams(graph)) {
      List<String> dependantChain = new ArrayList<>();
      dependantChain.add(rootNode.vajramId().vajramId());
      Map<String, Integer> ioNodeDepths = new HashMap<>();
      dfs(rootNode, graph, ioNodes, 0, dependantChain, ioNodeDepths);
    }
    return ioNodes;
  }

  private static Iterable<VajramDefinition> externallyInvocableVajrams(VajramKryonGraph graph) {
    return graph.kryonDefinitionRegistry().kryonDefinitions().values().stream()
        .filter(
            k -> {
              return k.tags()
                  .getAnnotationByType(ExternalInvocation.class)
                  .map(ExternalInvocation::allow)
                  .orElse(false);
            })
        .map(k -> graph.getVajramDefinition(vajramID(k.kryonId().value())))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  private static void dfs(
      VajramDefinition rootNode,
      VajramKryonGraph graph,
      Map<String, Map<Integer, List<String>>> ioNodes,
      int depth,
      List<String> dependantChain,
      Map<String, Integer> ioNodeDepths) {
    // find all the child nodes of rootNode
    // get the order of execution of inputDefinitions
    Map<VajramFacetDefinition, List<VajramFacetDefinition>> inputDefGraph = new HashMap<>();
    for (VajramFacetDefinition inputDef : getOrderedInputDef(rootNode, inputDefGraph)) {
      if (inputDef instanceof DependencyDef<?> dependency) {
        List<InputResolverDefinition> inputResolverDefinition =
            getInputResolverDefinition(rootNode, dependency);
        VajramID vajramId = (VajramID) dependency.dataAccessSpec();
        VajramDefinition childNode = graph.getVajramDefinition(vajramId).orElse(null);
        if (childNode != null) {
          if (inputDefGraph.get(inputDef) != null) {
            for (VajramFacetDefinition inputDef1 : inputDefGraph.get(inputDef)) {
              VajramID prerequisiteVajramId =
                  dependencyInputInChildNode(inputResolverDefinition, inputDef1);
              if (prerequisiteVajramId != null) {
                incrementTheLeafIONodeOfTheVajram(
                    graph.getVajramDefinition(prerequisiteVajramId).get(), graph, ioNodeDepths);
              }
            }
          }
          dependantChain.add(dependency.name());
          if (childNode.vajram() instanceof IOVajram<?>) {
            depth = ioNodeDepths.getOrDefault(childNode.vajramId().vajramId(), 0);
            ioNodes
                .computeIfAbsent(childNode.vajramId().vajramId(), k -> new HashMap<>())
                .computeIfAbsent(depth, k -> new ArrayList<>())
                .add(String.join(DEP_DELIMITER, dependantChain));
          }
          dfs(childNode, graph, ioNodes, depth, dependantChain, ioNodeDepths);
          if (inputDefGraph.get(inputDef) != null) {
            for (VajramFacetDefinition inputDef1 : inputDefGraph.get(inputDef)) {
              VajramID prerequisiteVajramId =
                  dependencyInputInChildNode(inputResolverDefinition, inputDef1);
              if (prerequisiteVajramId != null
                  && graph.getVajramDefinition(prerequisiteVajramId).isPresent()) {
                decrementTheLeafIONodeOfTheVajram(
                    graph.getVajramDefinition(prerequisiteVajramId).get(), graph, ioNodeDepths);
              }
            }
          }
          dependantChain.remove(dependantChain.size() - 1);
        }
      }
    }
  }

  private static void incrementTheLeafIONodeOfTheVajram(
      VajramDefinition node, VajramKryonGraph graph, Map<String, Integer> ioNodeDepth) {
    for (VajramFacetDefinition inputDef : node.vajram().getFacetDefinitions()) {
      if (inputDef instanceof DependencyDef<?> dependency) {
        VajramID vajramId = (VajramID) dependency.dataAccessSpec();
        Optional<VajramDefinition> childNode = graph.getVajramDefinition(vajramId);
        if (childNode.isPresent()) {
          if (childNode.get().vajram() instanceof IOVajram<?>) {
            ioNodeDepth.put(
                childNode.get().vajramId().vajramId(),
                ioNodeDepth.getOrDefault(childNode.get().vajramId().vajramId(), 0) + 1);
          } else {
            incrementTheLeafIONodeOfTheVajram(childNode.get(), graph, ioNodeDepth);
          }
        }
      }
    }
  }

  private static void decrementTheLeafIONodeOfTheVajram(
      VajramDefinition node, VajramKryonGraph graph, Map<String, Integer> ioNodeDepth) {
    for (VajramFacetDefinition inputDef : node.vajram().getFacetDefinitions()) {
      if (inputDef instanceof DependencyDef<?> dependency) {
        VajramID vajramId = (VajramID) dependency.dataAccessSpec();
        VajramDefinition childNode = graph.getVajramDefinition(vajramId).orElse(null);
        if (childNode != null) {
          if (childNode.vajram() instanceof IOVajram<?>) {
            ioNodeDepth.put(
                childNode.vajramId().vajramId(),
                ioNodeDepth.getOrDefault(childNode.vajramId().vajramId(), 0) - 1);
          } else {
            decrementTheLeafIONodeOfTheVajram(childNode, graph, ioNodeDepth);
          }
        }
      }
    }
  }

  private static @Nullable VajramID dependencyInputInChildNode(
      List<InputResolverDefinition> depInputs, VajramFacetDefinition inputDefinition) {
    for (InputResolverDefinition depInput : depInputs) {
      if (inputDefinition instanceof DependencyDef<?> dependency) {
        if (depInput.sources().contains(dependency.name())) {
          return (VajramID) dependency.dataAccessSpec();
        }
      }
    }
    return null;
  }

  private static List<InputResolverDefinition> getInputResolverDefinition(
      VajramDefinition rootNode, DependencyDef<?> dependency) {
    return rootNode.inputResolverDefinitions().stream()
        .filter(
            inputResolverDef ->
                inputResolverDef.resolutionTarget().dependencyName().equals(dependency.name()))
        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
  }

  private static Collection<VajramFacetDefinition> getOrderedInputDef(
      VajramDefinition rootNode, Map<VajramFacetDefinition, List<VajramFacetDefinition>> graph) {

    ImmutableCollection<InputResolverDefinition> resolverDefinitions =
        rootNode.inputResolverDefinitions();
    ImmutableCollection<VajramFacetDefinition> inputDefinitions =
        rootNode.vajram().getFacetDefinitions();
    for (InputResolverDefinition inputResolverDefinition : resolverDefinitions) {
      for (VajramFacetDefinition inputDefinition : inputDefinitions) {
        if (inputDefinition instanceof DependencyDef<?> dependency) {
          if (inputResolverDefinition.sources().contains(dependency.name())) {
            VajramFacetDefinition dependingVID =
                getInputDefinitionDep(
                    inputResolverDefinition.resolutionTarget().dependencyName(), inputDefinitions);
            if (dependingVID != null) {
              graph.putIfAbsent(dependingVID, new ArrayList<>());
              graph.get(dependingVID).add(inputDefinition);
            }
          }
        }
      }
    }
    Set<VajramFacetDefinition> visited = new HashSet<>();
    Queue<VajramFacetDefinition> queue = new LinkedList<>();
    for (VajramFacetDefinition vid : inputDefinitions) {
      if (vid instanceof DependencyDef<?>) {
        if (!visited.contains(vid)) {
          topologicalSortUtil(vid, visited, graph, queue);
        }
      }
    }
    return queue;
  }

  private static @Nullable VajramFacetDefinition getInputDefinitionDep(
      String dep, ImmutableCollection<VajramFacetDefinition> inputDefinitions) {
    for (VajramFacetDefinition inputDefinition : inputDefinitions) {
      if (inputDefinition instanceof DependencyDef<?> dependency) {
        if (dependency.name().equals(dep)) {
          return inputDefinition;
        }
      }
    }
    return null;
  }

  static void topologicalSortUtil(
      VajramFacetDefinition vid,
      Set<VajramFacetDefinition> visited,
      Map<VajramFacetDefinition, List<VajramFacetDefinition>> graph,
      Queue<VajramFacetDefinition> stack) {
    visited.add(vid);
    for (VajramFacetDefinition i : graph.getOrDefault(vid, new ArrayList<>())) {
      if (!visited.contains(i)) {
        topologicalSortUtil(i, visited, graph, stack);
      }
    }
    if (vid instanceof DependencyDef<?>) {
      stack.add(vid);
    }
  }

  public record BatcherContext(Vajram<?> vajram, LogicDecoratorContext logicDecoratorContext) {}

  @FunctionalInterface
  public interface BatchSizeSupplier {
    int getBatchSize(String vajramId);
  }
}
