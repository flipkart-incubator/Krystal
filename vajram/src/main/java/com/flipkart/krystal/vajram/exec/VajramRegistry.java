package com.flipkart.krystal.vajram.exec;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.krystex.NodeDefinition;
import com.flipkart.krystal.krystex.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.NonBlockingNodeDefinition;
import com.flipkart.krystal.vajram.ExecutionContext;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDefinitionException;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.ResolutionSources;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
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
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class VajramRegistry {

  private static final String SUFFIX_CHARS = "abcdefghjkmnpqrstuvwxy3456789";
  public static final int RANDOM_SUFFIX_LENGTH = 5;

  private final Random random = new Random();
  private final Map<String, VajramDefinition> vajramDefinitions = new HashMap<>();
  private final VajramIndex vajramIndex = new VajramIndex();
  private final NodeDefinitionRegistry nodeDefinitionRegistry = new NodeDefinitionRegistry();

  private VajramRegistry() {}

  public static VajramRegistry loadFromClasspath(
      String packagePrefix, Iterable<Vajram<?>> vajrams) {
    VajramRegistry vajramRegistry = new VajramRegistry();
    VajramLoader.loadVajramsFromClassPath(packagePrefix).forEach(vajramRegistry::registerVajram);
    vajrams.forEach(vajramRegistry::registerVajram);
    return vajramRegistry;
  }

  public Optional<VajramDefinition> getVajramDefinition(String vajramId) {
    return Optional.ofNullable(vajramDefinitions.get(vajramId));
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
   * Constructs a DAG that connects every Vajram with its dependencies which is a necessary step for
   * executing vajrams.
   *
   * <p>This method should be called once all necessary vajrams have been registered using the
   * {@link #registerVajram(Vajram)} method. If a dependency of a vajram is not registered before
   * this step, this method will throw an exception.
   *
   * @return
   */
  public NodeDefinition<?> getExecutionNode(VajramDefinition vajramDefinition) {
    return createSubGraphForVajram(vajramDefinition).vajramLogicNodeDefinition();
  }

  private SubgraphCreationResult createSubGraphForVajram(Vajram<?> vajram) {
    return createSubGraphForVajram(vajramDefinitions.get(vajram.getId()));
  }

  /**
   * Creates the node graph for the given vajram and its dependencies (by recursively calling this
   * same method) and returns the node definitions representing the input resolvers and main
   * vajramLogic of this vajram. These returned nodes can be used by the caller of this method to
   * bind them as dependants of other input resolvers created by the caller. This way, recursively
   * the complete execution graph is constructed.
   *
   * @param vajramDefinition
   * @return
   */
  // TODO Handle case were input resolvers bind from dependencies (sequential dependency in vajrams)
  @NonNull
  private SubgraphCreationResult createSubGraphForVajram(VajramDefinition vajramDefinition) {

    if (!vajramDefinition.getVajram().isBlockingVajram()) {
      InputResolverCreationResult inputResolverCreationResult =
          createNodeDefinitionsForInputResolvers(vajramDefinition);
      ImmutableMap<String, ImmutableMap<String, String>> inputResolverTargets =
          inputResolverCreationResult.inputResolverTargets();

      ImmutableMap<String, String> depNameToProviderNode =
          createSubGraphsForDependencies(vajramDefinition, inputResolverTargets);

      NonBlockingNodeDefinition<CompletableFuture<?>> vajramLogicNodeDefinition =
          createVajramLogicNodeDefinition(vajramDefinition, depNameToProviderNode);

      return new SubgraphCreationResult(
          vajramDefinition,
          vajramLogicNodeDefinition,
          inputResolverCreationResult.resolverDefinitions());
    } else {
      // TODO implement graph creation for blocking node
    }
    // TODO return the proper value
    return null;
  }

  private InputResolverCreationResult createNodeDefinitionsForInputResolvers(
      VajramDefinition vajramDefinition) {
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
                      nodeDefinitionRegistry.newUnboundNonBlockingBatchNode(
                          "v(%s):dep(%s):inputResolver(%s):%s"
                              .formatted(
                                  vajramId,
                                  dependencyName,
                                  String.join(":", resolvedInputNames),
                                  generateRandomSuffix()),
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

  private NonBlockingNodeDefinition<CompletableFuture<?>> createVajramLogicNodeDefinition(
      VajramDefinition vajramDefinition, ImmutableMap<String, String> depNameToProviderNode) {
    String vajramId = vajramDefinition.getVajram().getId();
    List<VajramInputDefinition> inputDefinitions =
        vajramDefinition.getVajram().getInputDefinitions();
    // Step 4: Create and register node for the main vajram logic
    NonBlockingNodeDefinition<CompletableFuture<?>> vajramLogicNodeDefinition =
        nodeDefinitionRegistry.newNonBlockingNode(
            "n(v(%s):vajramLogic:{%s})".formatted(vajramId, generateRandomSuffix()),
            depNameToProviderNode,
            dependencyValues -> {
              Map<String, Object> map = new HashMap<>();
              for (VajramInputDefinition inputDefinition : inputDefinitions) {
                String inputName = inputDefinition.name();
                String providerNodeName = depNameToProviderNode.get(inputName);
                if (inputDefinition instanceof Input<?> input) {
                  if (input.resolvableBy().contains(ResolutionSources.REQUEST)) {
                    if (providerNodeName == null
                        || dependencyValues.get(providerNodeName) == null) {
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
                      map.put(inputName, dependencyValues.get(providerNodeName));
                    }
                  }
                } else if (inputDefinition instanceof Dependency) {
                  map.put(inputName, dependencyValues.get(providerNodeName));
                }
              }
              return vajramDefinition.getVajram().execute(new ExecutionContext(map));
            });
    return vajramLogicNodeDefinition;
  }

  private ImmutableMap<String, String> createSubGraphsForDependencies(
      VajramDefinition vajramDefinition,
      ImmutableMap<String, ImmutableMap<String, String>> inputResolverTargets) {
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
      Map<DataAccessSpec, SubgraphCreationResult> dependencySubGraphs = new HashMap<>();
      dependencyVajrams.forEach(
          (dependencySpec, depVajram) ->
              dependencySubGraphs.put(dependencySpec, createSubGraphForVajram(depVajram)));
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
            "v(%s):dep(%s):n(adaptor):%s"
                .formatted(vajramId, dependencyName, generateRandomSuffix());
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
      Map<DataAccessSpec, SubgraphCreationResult> dependencySubGraphs) {
    ImmutableMap<String, String> inputProviderNodesForThisDependency =
        requireNonNull(inputResolverTargets.getOrDefault(dependencyName, ImmutableMap.of()));
    for (SubgraphCreationResult subGraph : dependencySubGraphs.values()) {
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
      ImmutableList<ResolverDefinition> subgraphResolvers = subGraph.resolverInputs();
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

  private String generateRandomSuffix() {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
      stringBuilder.append(SUFFIX_CHARS.charAt(random.nextInt(SUFFIX_CHARS.length())));
    }
    return stringBuilder.toString();
  }

  private record SubgraphCreationResult(
      VajramDefinition vajramDefinition,
      NodeDefinition<?> vajramLogicNodeDefinition,
      ImmutableList<ResolverDefinition> resolverInputs) {}

  private record ResolverDefinition(
      NodeDefinition<?> nodeDefinition, ImmutableSet<String> boundFrom) {}

  private record InputResolverCreationResult(
      ImmutableList<ResolverDefinition> resolverDefinitions,
      ImmutableMap<String, ImmutableMap<String, String>> inputResolverTargets) {}
}
