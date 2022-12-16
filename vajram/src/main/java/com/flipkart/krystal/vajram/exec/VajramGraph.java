package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.vajram.exec.Utils.toInputValues;
import static com.flipkart.krystal.vajram.exec.Utils.toNodeInputs;
import static com.flipkart.krystal.vajram.exec.Utils.toSingleValue;
import static com.flipkart.krystal.vajram.exec.VajramLoader.loadVajramsFromClassPath;
import static com.flipkart.krystal.vajram.inputs.ResolutionSources.REQUEST;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.krystex.node.IOLogicDefinition;
import com.flipkart.krystal.krystex.MultiResultFuture;
import com.flipkart.krystal.krystex.node.NodeDefinition;
import com.flipkart.krystal.krystex.node.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeDecorator;
import com.flipkart.krystal.krystex.node.NodeLogicDefinition;
import com.flipkart.krystal.krystex.node.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.krystex.node.NodeInputs;
import com.flipkart.krystal.krystex.node.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.ResolverDefinition;
import com.flipkart.krystal.krystex.SingleValue;
import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.ModulatedExecutionContext;
import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDefinitionException;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputResolverDefinition;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.ResolutionSources;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputModulator;
import com.flipkart.krystal.vajram.modulation.InputModulator.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.utils.RandomStringGenerator;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

/** The execution graph encompassing all registered vajrams. */
public final class VajramGraph {

  private static final int NODE_ID_SUFFIX_LENGTH = 5;
  public static final String VAJRAM_INPUT_MODULATION_GROUP = "vajram_input_modulation_group";
  public static final String APPLICATION_REQUEST_CONTEXT_KEY = "application_request_context";

  @Getter
  private final NodeDefinitionRegistry clusterDefinitionRegistry =
      new NodeDefinitionRegistry(new LogicDefinitionRegistry());

  private final Map<VajramID, VajramDefinition> vajramDefinitions = new LinkedHashMap<>();
  /** These are those call graphs of a vajram where no other vajram depends on this. */
  private final Map<VajramID, NodeDefinition> independentVajramDags = new LinkedHashMap<>();
  /** VajramDAGs which correspond to every call graph that vajram is part of. */
  private final Map<VajramID, NodeDefinition> allVajramDags = new LinkedHashMap<>();

  private final VajramIndex vajramIndex = new VajramIndex();
  private final RandomStringGenerator randomStringGenerator = RandomStringGenerator.instance();

  private final Map<VajramID, Supplier<InputModulator<Object, Object>>> inputModulators =
      new LinkedHashMap<>();

  private VajramGraph() {}

  public static VajramGraph loadFromClasspath(String... packagePrefix) {
    return loadFromClasspath(packagePrefix, ImmutableList.of());
  }

  public static VajramGraph loadFromClasspath(
      String[] packagePrefixes, Iterable<Vajram<?>> vajrams) {
    VajramGraph vajramGraph = new VajramGraph();
    for (String packagePrefix : packagePrefixes) {
      loadVajramsFromClassPath(packagePrefix).forEach(vajramGraph::registerVajram);
    }
    vajrams.forEach(vajramGraph::registerVajram);
    return vajramGraph;
  }

  public void registerInputModulator(
      VajramID vajramID, Supplier<InputModulator<Object, Object>> inputModulator) {
    inputModulators.put(vajramID, inputModulator);
    Vajram<?> vajram = vajramDefinitions.get(vajramID).getVajram();
    if (vajram instanceof IOVajram<?> ioVajram) {
      Supplier<NodeDecorator<Object>> inputModulationDecoratorSupplier =
          getInputModulationDecoratorSupplier(ioVajram, inputModulator);
      NodeDefinition nodeDefinition = allVajramDags.get(vajramID);
      if (nodeDefinition != null) {
        clusterDefinitionRegistry
            .nodeDefinitionRegistry()
            .get(nodeDefinition.logicNode())
            .registerRequestScopedNodeDecorator(inputModulationDecoratorSupplier);
      }
    }
  }
  /**
   * Registers vajrams that need to be executed at a later point. This is a necessary step for
   * vajram execution.
   *
   * @param vajram The vajram to be registered for future execution.
   */
  void registerVajram(Vajram vajram) {
    if (vajramDefinitions.containsKey(vajram.getId())) {
      return;
    }
    vajramDefinitions.put(vajram.getId(), new VajramDefinition(vajram));
    vajramIndex.add(vajram);
  }

  /**
   * Creates the node graph for the given vajram and its dependencies (by recursively calling this
   * same method) and returns the node definitions representing the input resolvers and main
   * vajramLogic of this vajram. These returned nodes can be used by the caller of this method to
   * bind them as dependants of other input resolvers created by the caller. This way, recursively
   * the complete execution graph is constructed.
   *
   * <p>This method should be called once all necessary vajrams have been registered using the *
   * {@link #registerVajram(Vajram)} method. If a dependency of a vajram is not registered before *
   * this step, this method will throw an exception.
   *
   * @param vajramId The id of the vajram to execute.
   */
  // TODO Handle case were input resolvers bind from dependencies (sequential dependency in vajrams)
  public NodeId getExecutable(VajramID vajramId) {
    return independentVajramDags
        .computeIfAbsent(
            vajramId,
            v -> _getVajramExecutionGraph(getVajramDefinition(v).orElseThrow().getVajram()))
        .nodeId();
  }

  @NonNull
  private NodeDefinition _getVajramExecutionGraph(Vajram vajram) {
    VajramDefinition vajramDefinition = getVajramDefinition(vajram.getId()).orElseThrow();
    InputResolverCreationResult inputResolverCreationResult =
        createNodeDefinitionsForInputResolvers(vajramDefinition);

    ImmutableMap<String, NodeDefinition> depNameToSubgraph =
        createSubGraphsForDependencies(vajramDefinition);

    ImmutableMap<String, NodeId> depNameToProviderNode =
        depNameToSubgraph.entrySet().stream()
            .collect(toImmutableMap(Entry::getKey, e -> e.getValue().nodeId()));
    NodeLogicDefinition<?> vajramLogicNodeLogicDefinition = createVajramLogicNodeDefinition(vajramDefinition);

    NodeDefinition nodeDefinition =
        clusterDefinitionRegistry.newClusterDefinition(
            vajram.getId().vajramId(),
            vajramLogicNodeLogicDefinition.nodeId(),
            depNameToProviderNode,
            inputResolverCreationResult.resolverDefinitions());
    allVajramDags.put(vajram.getId(), nodeDefinition);
    return nodeDefinition;
  }

  private InputResolverCreationResult createNodeDefinitionsForInputResolvers(
      VajramDefinition vajramDefinition) {
    Vajram<?> vajram = vajramDefinition.getVajram();
    VajramID vajramId = vajram.getId();

    // Create node definitions for all input resolvers defined in this vajram
    List<InputResolverDefinition> inputResolvers =
        new ArrayList<>(vajramDefinition.getInputResolverDefinitions());
    ImmutableList<ResolverDefinition> resolverDefinitions =
        inputResolvers.stream()
            .map(
                inputResolver -> {
                  String dependencyName = inputResolver.resolutionTarget().dependencyName();
                  ImmutableSet<String> resolvedInputNames =
                      inputResolver.resolutionTarget().inputNames();
                  ImmutableSet<String> sources = inputResolver.sources();
                  String resolverNodeId =
                      "%s:dep(%s):%s(%s):%s"
                          .formatted(
                              vajramId,
                              dependencyName,
                              "inputResolver",
                              String.join(",", resolvedInputNames),
                              generateNodeSuffix());
                  ComputeLogicDefinition<?> inputResolverNode =
                      clusterDefinitionRegistry
                          .nodeDefinitionRegistry()
                          .newNonBlockingBatchNode(
                              resolverNodeId,
                              sources,
                              dependencyValues ->
                                  vajram
                                      .resolveInputOfDependency(
                                          dependencyName,
                                          resolvedInputNames,
                                          new ExecutionContextMap(toInputValues(dependencyValues)))
                                      .stream()
                                      .map(Utils::toNodeInputs)
                                      .collect(toImmutableList()));
                  return new ResolverDefinition(
                      new NodeLogicId(resolverNodeId), sources, dependencyName, resolvedInputNames);
                })
            .collect(toImmutableList());
    return new InputResolverCreationResult(resolverDefinitions);
  }

  private NodeLogicDefinition<?> createVajramLogicNodeDefinition(VajramDefinition vajramDefinition) {
    VajramID vajramId = vajramDefinition.getVajram().getId();
    ImmutableCollection<VajramInputDefinition> inputDefinitions =
        vajramDefinition.getVajram().getInputDefinitions();
    Set<String> inputs =
        inputDefinitions.stream().map(VajramInputDefinition::name).collect(Collectors.toSet());
    NodeLogicId vajramLogicNodeName =
        new NodeLogicId("n(v(%s):vajramLogic:%s)".formatted(vajramId, generateNodeSuffix()));
    // Step 4: Create and register node for the main vajram logic
    if (vajramDefinition.getVajram() instanceof NonBlockingVajram<?> nonBlockingVajram) {
      return clusterDefinitionRegistry
          .nodeDefinitionRegistry()
          .newNonBlockingBatchNode(
              vajramLogicNodeName.asString(),
              inputs,
              dependencyValues ->
                  ImmutableList.of(
                      nonBlockingVajram.executeNonBlocking(
                          createExecutionContext(vajramId, inputDefinitions, dependencyValues))));
    } else if (vajramDefinition.getVajram() instanceof IOVajram<?> ioVajram) {
      //noinspection unchecked
      var inputsConvertor = (InputsConverter<Object, Object, Object>) ioVajram.getInputsConvertor();
      IOLogicDefinition<?> ioNodeDefinition =
          clusterDefinitionRegistry
              .nodeDefinitionRegistry()
              .newIONodeDefinition(
                  vajramLogicNodeName,
                  inputs,
                  dependencyValues -> {
                    List<Object> enrichedRequests =
                        dependencyValues.stream()
                            .map(Utils::toInputValues)
                            .map(inputsConvertor::enrichedRequest)
                            .toList();
                    if (enrichedRequests.isEmpty()) {
                      return ImmutableMap.of();
                    }
                    ModulatedInput<Object, Object> modulatedRequest =
                        new ModulatedInput<>(
                            enrichedRequests.stream()
                                .map(inputsConvertor::inputsNeedingModulation)
                                .collect(toImmutableList()),
                            inputsConvertor.commonInputs(enrichedRequests.iterator().next()));
                    return ioVajram
                        .execute(new ModulatedExecutionContext(modulatedRequest))
                        .entrySet()
                        .stream()
                        .collect(
                            toImmutableMap(
                                e -> toNodeInputs(inputsConvertor.toMap(e.getKey())),
                                e ->
                                    new MultiResultFuture<>(
                                        e.getValue().thenApply(ImmutableList::of))));
                  });
      enableInputModulation(ioNodeDefinition, ioVajram);
      return ioNodeDefinition;
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private <T> void enableInputModulation(IOLogicDefinition<T> nodeDefinition, IOVajram<?> ioVajram) {
    Supplier<InputModulator<Object, Object>> inputModulationDecorator =
        inputModulators.get(ioVajram.getId());
    if (inputModulationDecorator != null) {
      nodeDefinition.registerRequestScopedNodeDecorator(
          getInputModulationDecoratorSupplier(ioVajram, inputModulationDecorator));
    }
  }

  private static <T> Supplier<NodeDecorator<T>> getInputModulationDecoratorSupplier(
      IOVajram<?> ioVajram, Supplier<InputModulator<Object, Object>> inputModulationDecorator) {
    @SuppressWarnings("unchecked")
    InputsConverter<Object, Object, Object> inputsConvertor =
        (InputsConverter<Object, Object, Object>) ioVajram.getInputsConvertor();
    return () -> new InputModulationDecorator<>(inputModulationDecorator.get(), inputsConvertor);
  }

  private static ExecutionContextMap createExecutionContext(
      VajramID vajramId,
      ImmutableCollection<VajramInputDefinition> inputDefinitions,
      NodeInputs dependencyValues) {
    Map<String, com.flipkart.krystal.vajram.inputs.SingleValue<?>> map = new HashMap<>();
    for (VajramInputDefinition inputDefinition : inputDefinitions) {
      String inputName = inputDefinition.name();
      if (inputDefinition instanceof Input<?> input) {
        if (input.resolvableBy().contains(REQUEST)) {
          if (dependencyValues.values().get(inputName) == null
              || SingleValue.empty().equals(dependencyValues.getValue(inputName))) {
            // Input was not resolved by another node. Check if it is resolvable
            // by SESSION
            if (input.resolvableBy().contains(ResolutionSources.SESSION)) {
              // TODO handle session provided inputs
            } else {
              throw new VajramDefinitionException(
                  "Input: "
                      + input.name()
                      + " of vajram: "
                      + vajramId
                      + " was not resolved by the request.");
            }
          } else {
            map.put(inputName, toSingleValue(dependencyValues.getValue(inputName)));
          }
        }
      } else if (inputDefinition instanceof Dependency) {
        map.put(inputName, toSingleValue(dependencyValues.getValue(inputName)));
      }
    }
    return new ExecutionContextMap(new InputValues(ImmutableMap.copyOf(map)));
  }

  private ImmutableMap<String, NodeDefinition> createSubGraphsForDependencies(
      VajramDefinition vajramDefinition) {
    List<Dependency> dependencies = new ArrayList<>();
    for (VajramInputDefinition vajramInputDefinition :
        vajramDefinition.getVajram().getInputDefinitions()) {
      if (vajramInputDefinition instanceof Dependency definition) {
        dependencies.add(definition);
      }
    }
    Map<String, NodeDefinition> depNameToProviderNode = new HashMap<>();
    // Create and register sub graphs for dependencies of this vajram
    for (Dependency dependency : dependencies) {
      var accessSpec = dependency.dataAccessSpec();
      String dependencyName = dependency.name();
      AccessSpecMatchingResult<DataAccessSpec> accessSpecMatchingResult =
          vajramIndex.getVajrams(accessSpec);
      if (accessSpecMatchingResult.hasUnsuccessfulMatches()) {
        throw new VajramDefinitionException(
            "Unable to find vajrams for accessSpecs %s"
                .formatted(accessSpecMatchingResult.unsuccessfulMatches()));
      }
      ImmutableMap<DataAccessSpec, Vajram> dependencyVajrams =
          accessSpecMatchingResult.successfulMatches();
      if (dependencyVajrams.size() > 1) {
        throw new UnsupportedOperationException();
      }
      Vajram dependencyVajram = dependencyVajrams.values().iterator().next();
      NodeDefinition clusterDefinition = _getVajramExecutionGraph(dependencyVajram);

      depNameToProviderNode.put(dependencyName, clusterDefinition);
    }
    return ImmutableMap.copyOf(depNameToProviderNode);
  }

  private record InputResolverCreationResult(
      ImmutableList<ResolverDefinition> resolverDefinitions) {}

  private String generateNodeSuffix() {
    return randomStringGenerator.generateRandomString(NODE_ID_SUFFIX_LENGTH);
  }

  private Optional<VajramDefinition> getVajramDefinition(VajramID vajramId) {
    return Optional.ofNullable(vajramDefinitions.get(vajramId));
  }
}
