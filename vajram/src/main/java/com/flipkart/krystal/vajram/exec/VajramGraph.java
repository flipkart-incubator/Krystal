package com.flipkart.krystal.vajram.exec;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.krystex.NodeDefinition;
import com.flipkart.krystal.krystex.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.NonBlockingNodeDefinition;
import com.flipkart.krystal.vajram.ExecutionContext;
import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDefinitionException;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.exec.VajramDAG.ResolverDefinition;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.ResolutionSources;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.utils.RandomStringGenerator;
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
import java.util.stream.Collectors;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

/** The execution graph encompassing all registered vajrams. */
public final class VajramGraph {

  private static final int NODE_ID_SUFFIX_LENGTH = 5;

  @Getter
  private final NodeDefinitionRegistry nodeDefinitionRegistry = new NodeDefinitionRegistry();

  private final Map<String, VajramDefinition> vajramDefinitions = new HashMap<>();
  private final VajramIndex vajramIndex = new VajramIndex();
  private final RandomStringGenerator randomStringGenerator = RandomStringGenerator.instance();

  private VajramGraph() {}

  public static VajramGraph loadFromClasspath(String packagePrefix) {
    return loadFromClasspath(packagePrefix, ImmutableList.of());
  }

  public static VajramGraph loadFromClasspath(String packagePrefix, Iterable<Vajram<?>> vajrams) {
    VajramGraph vajramGraph = new VajramGraph();
    VajramLoader.loadVajramsFromClassPath(packagePrefix).forEach(vajramGraph::registerVajram);
    vajrams.forEach(vajramGraph::registerVajram);
    return vajramGraph;
  }

  /**
   * Registers vajrams that need to be executed at a later point. This is a necessary step for
   * vajram execution.
   *
   * @param vajram The vajram to be registered for future execution.
   */
  void registerVajram(Vajram<?> vajram) {
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
   * @param vajramId
   * @return
   */
  // TODO Handle case were input resolvers bind from dependencies (sequential dependency in vajrams)
  @NonNull
  public <T> VajramDAG<T> createVajramDAG(String vajramId) {
    //noinspection unchecked
    return (VajramDAG<T>)
        _getVajramExecutionGraph(getVajramDefinition(vajramId).orElseThrow().getVajram());
  }

  @NonNull
  private VajramDAG<?> _getVajramExecutionGraph(Vajram<?> vajram) {
    VajramDAG<?> vajramDAG =
        new VajramDAG<>(getVajramDefinition(vajram.getId()).orElseThrow(), nodeDefinitionRegistry);
    if (!vajram.isBlockingVajram()) {
      InputResolverCreationResult inputResolverCreationResult =
          createNodeDefinitionsForInputResolvers(vajramDAG);
      ImmutableMap<String, ImmutableMap<String, String>> inputResolverTargets =
          inputResolverCreationResult.inputResolverTargets();

      ImmutableMap<String, String> depNameToProviderNode =
          createSubGraphsForDependencies(vajramDAG, inputResolverTargets);

      NonBlockingNodeDefinition<?> vajramLogicNodeDefinition =
          createVajramLogicNodeDefinition(vajramDAG, depNameToProviderNode);

      return new VajramDAG<>(
          vajramDAG.vajramDefinition(),
          vajramLogicNodeDefinition,
          inputResolverCreationResult.resolverDefinitions(),
          depNameToProviderNode,
          vajramDAG.nodeDefinitionRegistry());
    } else {
      // TODO implement graph creation for blocking node
      return null;
    }
  }

  private InputResolverCreationResult createNodeDefinitionsForInputResolvers(
      VajramDAG<?> vajramDAG) {
    VajramDefinition vajramDefinition = vajramDAG.vajramDefinition();
    NodeDefinitionRegistry nodeDefinitionRegistry = vajramDAG.nodeDefinitionRegistry();
    Vajram<?> vajram = vajramDefinition.getVajram();
    String vajramId = vajram.getId();
    Map</*dependency name*/ String, Map</*input name*/ String, /*node id*/ String>>
        inputResolverTargets = new LinkedHashMap<>();
    // Create node definitions for all input resolvers defined in this vajram
    ImmutableList<ResolverDefinition> resolverDefinitions =
        vajramDefinition.getInputResolvers().stream()
            .map(
                inputResolver -> {
                  String dependencyName = inputResolver.resolutionTarget().dependencyName();
                  ImmutableSet<String> resolvedInputNames =
                      inputResolver.resolutionTarget().inputNames();
                  ImmutableSet<String> sources = inputResolver.sources();
                  NonBlockingNodeDefinition<?> inputResolverNode =
                      nodeDefinitionRegistry.newNonBlockingBatchNode(
                          "v(%s):dep(%s):inputResolver(%s):%s"
                              .formatted(
                                  vajramId,
                                  dependencyName,
                                  String.join(":", resolvedInputNames),
                                  generateNodeSuffix()),
                          dependencyValues -> {
                            Map<String, Object> map = new HashMap<>();
                            sources.forEach(s -> map.put(s, dependencyValues.get(s)));
                            return vajram.resolveInputOfDependency(
                                dependencyName, resolvedInputNames, new ExecutionContext(map));
                          });
                  Map<String, String> inputNameToProviderNode =
                      inputResolverTargets.computeIfAbsent(
                          dependencyName, s -> new LinkedHashMap<>());
                  sources.forEach(s -> inputNameToProviderNode.put(s, inputResolverNode.nodeId()));
                  return new ResolverDefinition(inputResolverNode, sources);
                })
            .collect(toImmutableList());
    return new InputResolverCreationResult(
        resolverDefinitions,
        inputResolverTargets.entrySet().stream()
            .collect(toImmutableMap(Entry::getKey, o -> ImmutableMap.copyOf(o.getValue()))));
  }

  private NonBlockingNodeDefinition<?> createVajramLogicNodeDefinition(
      VajramDAG<?> vajramDAG, ImmutableMap<String, String> depNameToProviderNode) {
    VajramDefinition vajramDefinition = vajramDAG.vajramDefinition();
    NodeDefinitionRegistry nodeDefinitionRegistry = vajramDAG.nodeDefinitionRegistry();
    String vajramId = vajramDefinition.getVajram().getId();
    List<VajramInputDefinition> inputDefinitions =
        vajramDefinition.getVajram().getInputDefinitions();
    // Step 4: Create and register node for the main vajram logic
    if (vajramDefinition.getVajram() instanceof NonBlockingVajram<?> nonBlockingVajram) {
      return nodeDefinitionRegistry.newNonBlockingBatchNode(
          "n(v(%s):vajramLogic:%s)".formatted(vajramId, generateNodeSuffix()),
          inputDefinitions.stream().map(VajramInputDefinition::name).collect(Collectors.toSet()),
          depNameToProviderNode,
          dependencyValues -> {
            Map<String, Object> map = new HashMap<>();
            for (VajramInputDefinition inputDefinition : inputDefinitions) {
              String inputName = inputDefinition.name();
              if (inputDefinition instanceof Input<?> input) {
                if (input.resolvableBy().contains(ResolutionSources.REQUEST)) {
                  if (dependencyValues.get(inputName) == null
                      || Objects.equals(dependencyValues.get(inputName), Optional.empty())) {
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
                    map.put(inputName, dependencyValues.get(inputName));
                  }
                }
              } else if (inputDefinition instanceof Dependency) {
                map.put(inputName, dependencyValues.get(inputName));
              }
            }
            return nonBlockingVajram.executeNonBlocking(new ExecutionContext(map));
          });
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private ImmutableMap<String, String> createSubGraphsForDependencies(
      VajramDAG<?> vajramDAG,
      ImmutableMap<String, ImmutableMap<String, String>> inputResolverTargets) {
    VajramDefinition vajramDefinition = vajramDAG.vajramDefinition();
    NodeDefinitionRegistry nodeDefinitionRegistry = vajramDAG.nodeDefinitionRegistry();
    String vajramId = vajramDefinition.getVajram().getId();
    List<Dependency> dependencies = new ArrayList<>();
    for (VajramInputDefinition vajramInputDefinition :
        vajramDefinition.getVajram().getInputDefinitions()) {
      if (vajramInputDefinition instanceof Dependency definition) {
        dependencies.add(definition);
      }
    }
    Map<String, String> depNameToProviderNode = new HashMap<>();
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
      ImmutableMap<DataAccessSpec, Vajram<?>> dependencyVajrams =
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
          accessSpecMatchingResult.needsAdaption()) {
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
            dependencyValues -> accessSpec.adapt(dependencyValues.values()));
        depNameToProviderNode.put(dependencyName, nodeId);
      } else {
        depNameToProviderNode.put(
            dependencyName,
            dependencySubGraphs.values().iterator().next().vajramLogicNodeDefinition().nodeId());
      }
    }
    return ImmutableMap.copyOf(depNameToProviderNode);
  }

  private static void addInputResolversAsProvidersForSubGraphNodes(
      String vajramId,
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
        NodeDefinition<?> nodeDefinition = subgraphResolver.nodeDefinition();
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

  private Optional<VajramDefinition> getVajramDefinition(String vajramId) {
    return Optional.ofNullable(vajramDefinitions.get(vajramId));
  }
}
