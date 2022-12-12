package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.vajram.exec.VajramLoader.loadVajramsFromClassPath;
import static com.flipkart.krystal.vajram.inputs.ResolutionSources.REQUEST;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.krystex.IONodeDefinition;
import com.flipkart.krystal.krystex.MultiResult;
import com.flipkart.krystal.krystex.NodeDecorator;
import com.flipkart.krystal.krystex.NodeDefinition;
import com.flipkart.krystal.krystex.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.NodeInputs;
import com.flipkart.krystal.krystex.NonBlockingNodeDefinition;
import com.flipkart.krystal.krystex.nodecluster.DefaultNodeCluster;
import com.flipkart.krystal.krystex.nodecluster.NodeClusterRegistry;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.ModulatedExecutionContext;
import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDefinitionException;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.exec.VajramDAG.ResolverDefinition;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

/** The execution graph encompassing all registered vajrams. */
public final class VajramGraph<C extends ApplicationRequestContext> {

  private static final int NODE_ID_SUFFIX_LENGTH = 5;
  public static final String VAJRAM_INPUT_MODULATION_GROUP = "vajram_input_modulation_group";
  public static final String APPLICATION_REQUEST_CONTEXT_KEY = "application_request_context";

  @Getter
  private final NodeDefinitionRegistry nodeDefinitionRegistry = new NodeDefinitionRegistry();

  private final NodeClusterRegistry nodeClusterRegistry = new NodeClusterRegistry();

  @Getter private final String applicationContextProviderNodeId;

  private final Map<VajramID, VajramDefinition> vajramDefinitions = new LinkedHashMap<>();
  /** These are those call graphs of a vajram where no other vajram depends on this. */
  private final Map<VajramID, VajramDAG<?>> independentVajramDags = new LinkedHashMap<>();
  /** VajramDAGs which correspond to every call graph that vajram is part of. */
  private final Map<VajramID, List<VajramDAG<?>>> allVajramDags = new LinkedHashMap<>();

  private final VajramIndex vajramIndex = new VajramIndex();
  private final RandomStringGenerator randomStringGenerator = RandomStringGenerator.instance();

  private final Map<VajramID, Supplier<InputModulator<Object, Object>>> inputModulators =
      new LinkedHashMap<>();

  private VajramGraph() {
    applicationContextProviderNodeId =
        nodeDefinitionRegistry
            .newNonBlockingNode(
                APPLICATION_REQUEST_CONTEXT_KEY,
                Set.of(APPLICATION_REQUEST_CONTEXT_KEY),
                nodeInputs -> nodeInputs.values().get(APPLICATION_REQUEST_CONTEXT_KEY))
            .nodeId();
  }

  public static <C extends ApplicationRequestContext> VajramGraph<C> loadFromClasspath(
      String... packagePrefix) {
    return loadFromClasspath(packagePrefix, ImmutableList.of());
  }

  public static <C extends ApplicationRequestContext> VajramGraph<C> loadFromClasspath(
      String[] packagePrefixes, Iterable<Vajram<?>> vajrams) {
    VajramGraph<C> vajramGraph = new VajramGraph<>();
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
      allVajramDags
          .getOrDefault(vajramID, List.of())
          .forEach(
              vajramDAG -> {
                //noinspection unchecked,rawtypes
                ((NodeDefinition) vajramDAG.vajramLogicNodeDefinition())
                    .registerRequestScopedNodeDecorator(
                        "vajram_input_modulation_group", inputModulationDecoratorSupplier);
              });
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
   * @param vajramRequestBuilder A function which can create the VajramRequest for the given vajram
   *     from the {@link ApplicationRequestContext}.
   */
  // TODO Handle case were input resolvers bind from dependencies (sequential dependency in vajrams)
  @NonNull
  public <T> VajramDAG<T> createVajramDAG(
      VajramID vajramId, Function<C, VajramRequest> vajramRequestBuilder) {
    //noinspection unchecked
    VajramDAG<T> vajramDAG =
        (VajramDAG<T>)
            independentVajramDags.computeIfAbsent(
                vajramId,
                v -> _getVajramExecutionGraph(getVajramDefinition(v).orElseThrow().getVajram()));
    Map<String, String> inputNameToNodeId = new LinkedHashMap<>();
    String requestBuilderNodeId = "vajram_request_builder:%s".formatted(vajramId);
    //noinspection unchecked
    nodeDefinitionRegistry.newNonBlockingNode(
        requestBuilderNodeId,
        ImmutableMap.of(APPLICATION_REQUEST_CONTEXT_KEY, applicationContextProviderNodeId),
        nodeInputs ->
            vajramRequestBuilder.apply(
                (C) nodeInputs.values().get(APPLICATION_REQUEST_CONTEXT_KEY)));
    Set<String> vajramInputNames =
        vajramDefinitions.get(vajramId).getVajram().getInputDefinitions().stream()
            .filter(vi -> vi instanceof Input<?>)
            .map(vi -> (Input<?>) vi)
            .filter(i -> i.resolvableBy().contains(REQUEST))
            .map(Input::name)
            .collect(Collectors.toSet());
    vajramInputNames.forEach(
        vajramInputName -> {
          String nodeId = "vajram_input_provider:v(%s):i(%s)".formatted(vajramId, vajramInputName);
          String requestBuilderInputName = "vajram_request_builder";
          nodeDefinitionRegistry.newNonBlockingNode(
              nodeId,
              ImmutableMap.of(requestBuilderInputName, requestBuilderNodeId),
              nodeInputs -> {
                VajramRequest vajramRequest =
                    (VajramRequest) nodeInputs.values().get(requestBuilderInputName);
                if (vajramRequest == null) {
                  return null;
                }
                return vajramRequest.asMap().get(vajramInputName);
              });
          inputNameToNodeId.put(vajramInputName, nodeId);
        });
    return vajramDAG.addProviderNodes(inputNameToNodeId);
  }

  @NonNull
  private VajramDAG<?> _getVajramExecutionGraph(Vajram vajram) {
    VajramDefinition vajramDefinition = getVajramDefinition(vajram.getId()).orElseThrow();
    InputResolverCreationResult inputResolverCreationResult =
        createNodeDefinitionsForInputResolvers(vajramDefinition);
    ImmutableMap<String, ImmutableMap<String, String>> inputResolverTargets =
        inputResolverCreationResult.inputResolverTargets();

    ImmutableMap<String, SubGraphResult> depNameToSubgraph =
        createSubGraphsForDependencies(vajramDefinition, inputResolverTargets);

    ImmutableMap<String, String> depNameToProviderNode =
        depNameToSubgraph.entrySet().stream()
            .collect(toImmutableMap(Entry::getKey, e -> e.getValue().providerNode()));
    NodeDefinition<?> vajramLogicNodeDefinition =
        createVajramLogicNodeDefinition(vajramDefinition, depNameToProviderNode);

//    DefaultNodeCluster<Object> objectDefaultNodeCluster = new DefaultNodeCluster<>();
    VajramDAG<?> vajramDAG =
        new VajramDAG<>(
            vajramDefinition,
            vajramLogicNodeDefinition,
            inputResolverCreationResult.resolverDefinitions(),
            depNameToProviderNode,
            nodeDefinitionRegistry);
    allVajramDags
        .computeIfAbsent(vajramDefinition.getVajram().getId(), k -> new ArrayList<>())
        .add(vajramDAG);
    return vajramDAG;
  }

  private InputResolverCreationResult createNodeDefinitionsForInputResolvers(
      VajramDefinition vajramDefinition) {
    Vajram<?> vajram = vajramDefinition.getVajram();
    VajramID vajramId = vajram.getId();
    Map</*dependency name*/ String, Map</*input name*/ String, /*node id*/ String>>
        inputResolverTargets = new LinkedHashMap<>();
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
                  String mainResolverNodeId =
                      "%s:dep(%s):%s(%s):%s"
                          .formatted(
                              vajramId,
                              dependencyName,
                              resolvedInputNames.size() > 1 ? "multiResolver" : "inputResolver",
                              String.join(",", resolvedInputNames),
                              generateNodeSuffix());
                  NonBlockingNodeDefinition<?> inputResolverNode =
                      nodeDefinitionRegistry.newNonBlockingBatchNode(
                          mainResolverNodeId,
                          dependencyValues -> {
                            Map<String, Object> map = new HashMap<>();
                            sources.forEach(s -> map.put(s, dependencyValues.values().get(s)));
                            ImmutableList<InputValues> inputValues =
                                vajram.resolveInputOfDependency(
                                    dependencyName,
                                    resolvedInputNames,
                                    new ExecutionContextMap(map));
                            if (resolvedInputNames.size() == 1) {
                              return inputValues.stream()
                                  .map(iv -> iv.values().get(resolvedInputNames.iterator().next()))
                                  .collect(toImmutableList());
                            } else {
                              return ImmutableList.copyOf(inputValues);
                            }
                          });
                  Map<String, String> inputNameToProviderNode =
                      inputResolverTargets.computeIfAbsent(
                          dependencyName, s -> new LinkedHashMap<>());
                  if (resolvedInputNames.size() > 1) {
                    ImmutableList<NodeDefinition<?>> extractorNodes =
                        resolvedInputNames.stream()
                            .map(
                                resolvedInputName -> {
                                  String multiResolutionData = "multi_resolver";
                                  String resolverNodeId =
                                      "%s:dep(%s):inputResolver(%s):%s"
                                          .formatted(
                                              vajramId,
                                              dependencyName,
                                              resolvedInputName,
                                              generateNodeSuffix());
                                  inputNameToProviderNode.put(resolvedInputName, resolverNodeId);
                                  return nodeDefinitionRegistry.newNonBlockingBatchNode(
                                      resolverNodeId,
                                      ImmutableSet.of(multiResolutionData),
                                      ImmutableMap.of(multiResolutionData, mainResolverNodeId),
                                      dependencyValues -> {
                                        InputValues o =
                                            (InputValues)
                                                dependencyValues.values().get(multiResolutionData);
                                        if (o == null) {
                                          return ImmutableList.of();
                                        }
                                        Object result = o.values().get(resolvedInputName);
                                        if (result == null) {
                                          return ImmutableList.of();
                                        }
                                        return ImmutableList.of(result);
                                      });
                                })
                            .collect(toImmutableList());
                    return new ResolverDefinition(inputResolverNode, extractorNodes, sources);
                  } else {
                    resolvedInputNames.forEach(
                        s -> inputNameToProviderNode.put(s, mainResolverNodeId));
                    return new ResolverDefinition(
                        inputResolverNode, ImmutableList.of(inputResolverNode), sources);
                  }
                })
            .collect(toImmutableList());
    return new InputResolverCreationResult(
        resolverDefinitions,
        inputResolverTargets.entrySet().stream()
            .collect(toImmutableMap(Entry::getKey, o -> ImmutableMap.copyOf(o.getValue()))));
  }

  private NodeDefinition<?> createVajramLogicNodeDefinition(
      VajramDefinition vajramDefinition, ImmutableMap<String, String> depNameToProviderNode) {
    VajramID vajramId = vajramDefinition.getVajram().getId();
    ImmutableCollection<VajramInputDefinition> inputDefinitions =
        vajramDefinition.getVajram().getInputDefinitions();
    Set<String> inputs =
        inputDefinitions.stream().map(VajramInputDefinition::name).collect(Collectors.toSet());
    String vajramLogicNodeName =
        "n(v(%s):vajramLogic:%s)".formatted(vajramId, generateNodeSuffix());
    // Step 4: Create and register node for the main vajram logic
    if (vajramDefinition.getVajram() instanceof NonBlockingVajram<?> nonBlockingVajram) {
      return nodeDefinitionRegistry.newNonBlockingBatchNode(
          vajramLogicNodeName,
          inputs,
          depNameToProviderNode,
          dependencyValues ->
              ImmutableList.of(
                  nonBlockingVajram.executeNonBlocking(
                      createExecutionContext(vajramId, inputDefinitions, dependencyValues))));
    } else if (vajramDefinition.getVajram() instanceof IOVajram<?> ioVajram) {
      //noinspection unchecked
      var inputsConvertor = (InputsConverter<Object, Object, Object>) ioVajram.getInputsConvertor();
      IONodeDefinition<?> ioNodeDefinition =
          nodeDefinitionRegistry.newIONodeDefinition(
              vajramLogicNodeName,
              inputs,
              depNameToProviderNode,
              dependencyValues -> {
                List<Object> enrichedRequests =
                    dependencyValues.stream()
                        .map(nodeInputs -> new InputValues(nodeInputs.values()))
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
                            e -> new NodeInputs(inputsConvertor.toMap(e.getKey()).values()),
                            e -> new MultiResult<>(e.getValue().thenApply(ImmutableList::of))));
              });
      enableInputModulation(ioNodeDefinition, ioVajram);
      return ioNodeDefinition;
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private <T> void enableInputModulation(IONodeDefinition<T> nodeDefinition, IOVajram<?> ioVajram) {
    Supplier<InputModulator<Object, Object>> inputModulationDecorator =
        inputModulators.get(ioVajram.getId());
    if (inputModulationDecorator != null) {
      nodeDefinition.registerRequestScopedNodeDecorator(
          VAJRAM_INPUT_MODULATION_GROUP,
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
    Map<String, Object> map = new HashMap<>();
    for (VajramInputDefinition inputDefinition : inputDefinitions) {
      String inputName = inputDefinition.name();
      if (inputDefinition instanceof Input<?> input) {
        if (input.resolvableBy().contains(REQUEST)) {
          if (dependencyValues.values().get(inputName) == null
              || Objects.equals(dependencyValues.values().get(inputName), Optional.empty())) {
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
            map.put(inputName, dependencyValues.values().get(inputName));
          }
        }
      } else if (inputDefinition instanceof Dependency) {
        map.put(inputName, dependencyValues.values().get(inputName));
      }
    }
    return new ExecutionContextMap(map);
  }

  record SubGraphResult(String providerNode, ImmutableList<VajramDAG<?>> vajramDAG) {}

  private ImmutableMap<String, SubGraphResult> createSubGraphsForDependencies(
      VajramDefinition vajramDefinition,
      ImmutableMap<String, ImmutableMap<String, String>> inputResolverTargets) {
    VajramID vajramId = vajramDefinition.getVajram().getId();
    List<Dependency> dependencies = new ArrayList<>();
    for (VajramInputDefinition vajramInputDefinition :
        vajramDefinition.getVajram().getInputDefinitions()) {
      if (vajramInputDefinition instanceof Dependency definition) {
        dependencies.add(definition);
      }
    }
    Map<String, SubGraphResult> depNameToProviderNode = new HashMap<>();
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
      Map<DataAccessSpec, VajramDAG<?>> dependencySubGraphs = new HashMap<>();
      dependencyVajrams.forEach(
          (dependencySpec, depVajram) ->
              dependencySubGraphs.put(dependencySpec, _getVajramExecutionGraph(depVajram)));
      addInputResolversAsProvidersForSubGraphNodes(
          vajramId, inputResolverTargets, dependencyName, dependencySubGraphs);

      if (dependencySubGraphs.size() > 1
          // Since this access spec is being powered by multiple vajrams, we will need to merge
          // the responses
          ||
          // Since some vajrams are giving more data than has been requested, we will need
          // to prune the data to prevent unnecessary data from leaking to the logic in this
          // vajram
          accessSpecMatchingResult.needsAdaptation()) {
        // Create adaptor node if vajram responses need to be adapted
        String nodeId =
            "v(%s):dep(%s):n(adaptor):%s".formatted(vajramId, dependencyName, generateNodeSuffix());
        nodeDefinitionRegistry.newNonBlockingNode(
            nodeId,
            // Set all nodes powering the dependency access spec as inputs to the adaptor node.
            dependencySubGraphs.entrySet().stream()
                .collect(
                    toImmutableMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().vajramLogicNodeDefinition().nodeId())),
            dependencyValues -> accessSpec.adapt(dependencyValues.values().values()));
        depNameToProviderNode.put(
            dependencyName,
            new SubGraphResult(nodeId, ImmutableList.copyOf(dependencySubGraphs.values())));
      } else {
        VajramDAG<?> subGraphDAG = dependencySubGraphs.values().iterator().next();
        depNameToProviderNode.put(
            dependencyName,
            new SubGraphResult(
                subGraphDAG.vajramLogicNodeDefinition().nodeId(), ImmutableList.of(subGraphDAG)));
      }
    }
    return ImmutableMap.copyOf(depNameToProviderNode);
  }

  private static void addInputResolversAsProvidersForSubGraphNodes(
      VajramID vajramId,
      ImmutableMap<String, ImmutableMap<String, String>> inputResolverTargets,
      String dependencyName,
      Map<DataAccessSpec, VajramDAG<?>> dependencySubGraphs) {
    ImmutableMap<String, String> inputProviderNodesForThisDependency =
        requireNonNull(inputResolverTargets.getOrDefault(dependencyName, ImmutableMap.of()));
    for (VajramDAG<?> subGraph : dependencySubGraphs.values()) {
      subGraph.vajramDefinition().getVajram().getInputDefinitions().stream()
          .filter(vajramInputDefinition -> vajramInputDefinition instanceof Input<?>)
          .map(VajramInputDefinition::name)
          .forEach(
              inputName -> {
                String providerNodeId = inputProviderNodesForThisDependency.get(inputName);
                if (providerNodeId == null) {
                  throw new IllegalStateException(
                      "Input: %s of dependency: %s of vajram: %s does not have a resolver"
                          .formatted(inputName, dependencyName, vajramId));
                }
                subGraph.vajramLogicNodeDefinition().addInputProvider(inputName, providerNodeId);
              });
      ImmutableList<ResolverDefinition> subgraphResolvers = subGraph.resolverDefinitions();
      for (ResolverDefinition subgraphResolver : subgraphResolvers) {
        NodeDefinition<?> nodeDefinition = subgraphResolver.resolverNode();
        subgraphResolver
            .boundFrom()
            .forEach(
                boundFromInput -> {
                  String providerNode = inputProviderNodesForThisDependency.get(boundFromInput);
                  if (providerNode != null) {
                    nodeDefinition.addInputProvider(boundFromInput, providerNode);
                  }
                });
      }
    }
  }

  private record InputResolverCreationResult(
      ImmutableList<ResolverDefinition> resolverDefinitions,
      ImmutableMap<String, ImmutableMap<String, String>> inputResolverTargets) {}

  private String generateNodeSuffix() {
    return randomStringGenerator.generateRandomString(NODE_ID_SUFFIX_LENGTH);
  }

  private Optional<VajramDefinition> getVajramDefinition(VajramID vajramId) {
    return Optional.ofNullable(vajramDefinitions.get(vajramId));
  }
}
