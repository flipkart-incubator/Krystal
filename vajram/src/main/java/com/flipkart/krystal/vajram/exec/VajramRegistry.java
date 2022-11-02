package com.flipkart.krystal.vajram.exec;

import static java.util.Collections.emptySet;

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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class VajramRegistry {

  private static final String CHARS = "abcdefghjkmnpqrstuvwxy3456789";
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
   */
  public void constructExecutionGraph() {
    vajramDefinitions.forEach(
        (vajramId, vajramDefinition) -> createSubGraphForVajram(vajramDefinition));
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
   * @see #constructExecutionGraph()
   * @param vajramDefinition
   * @return
   */
  private SubgraphCreationResult createSubGraphForVajram(VajramDefinition vajramDefinition) {
    Vajram<?> vajram = vajramDefinition.getVajram();
    String vajramId = vajram.getId();
    List<VajramInputDefinition> inputDefinitions = vajram.getInputDefinitions();
    List<Dependency> dependencies =
        inputDefinitions.stream()
            .filter(vajramInputDefinition -> vajramInputDefinition instanceof Dependency)
            .map(vajramInputDefinition -> (Dependency) vajramInputDefinition)
            .toList();

    if (!vajram.isBlockingVajram()) {
      // Step 1: Create and register node for the main vajram logic
      NonBlockingNodeDefinition<? extends CompletableFuture<?>> vajramLogicNodeDefinition =
          nodeDefinitionRegistry.newNonBlockingNode(
              "n(v(%s):vajramLogic:{%s})".formatted(vajramId, generateRandomSuffix()),
              emptySet(),
              dependencyValues1 -> {
                ImmutableMap<String, String> nodesForInput = vajramDefinition.getNodesForInputs();
                Map<String, Object> map1 = new HashMap<>();
                for (VajramInputDefinition inputDefinition : inputDefinitions) {
                  String inputName = inputDefinition.name();
                  String providerNodeName = nodesForInput.get(inputName);
                  if (inputDefinition instanceof Input<?> input) {
                    if (input.resolvableBy().contains(ResolutionSources.REQUEST)) {
                      if (providerNodeName == null
                          || dependencyValues1.get(providerNodeName) == null) {
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
                        map1.put(inputName, dependencyValues1.get(providerNodeName));
                      }
                    }
                  } else if (inputDefinition instanceof Dependency) {
                    map1.put(inputName, dependencyValues1.get(providerNodeName));
                  }
                }
                return vajramDefinition.getVajram().execute(new ExecutionContext(map1));
              });

      // Step 2: Create node definitions for all input resolvers defined in this vajram
      vajramDefinition
          .getInputResolvers()
          .forEach(
              inputResolver -> {
                String dependencyName = inputResolver.resolutionTarget().dependencyName();
                ImmutableSet<String> resolvedInputNames =
                    inputResolver.resolutionTarget().inputNames();
                nodeDefinitionRegistry.newNonBlockingNode(
                    "v(%s):dep(%s):inputResolver(%s):%s"
                        .formatted(
                            vajramId,
                            dependencyName,
                            String.join(":", resolvedInputNames),
                            generateRandomSuffix()),
                    emptySet(),
                    dependencyValues -> {
                      Map<String, Object> map = new HashMap<>();
                      ImmutableSet<String> sources = inputResolver.sources();
                      sources.forEach(s -> map.put(s, dependencyValues.get(s)));
                      return vajram.resolveInputOfDependency(
                          dependencyName, resolvedInputNames, new ExecutionContext(map));
                    });
              });

      // Step 3: Create and register sub graphs for dependencies of this vajram
      for (Dependency dependency : dependencies) {
        var accessSpec = dependency.dataAccessSpec();
        String dependencyName = dependency.name();
        ImmutableMap<DataAccessSpec, Vajram<?>> dependencyVajrams =
            vajramIndex.getVajrams(accessSpec);
        Map<DataAccessSpec, SubgraphCreationResult> dependencySubGraphs = new HashMap<>();
        dependencyVajrams.forEach(
            (dependencySpec, depVajram) ->
                dependencySubGraphs.put(dependencySpec, createSubGraphForVajram(depVajram)));
        if (dependencyVajrams.size() > 1) {
          nodeDefinitionRegistry.newNonBlockingNode(
              "v(%s):dep(%s):n(merger):%s"
                  .formatted(
                      vajramDefinition.getVajram().getId(), dependencyName, generateRandomSuffix()),
              dependencySubGraphs.values().stream()
                  .map(
                      subgraphCreationResult ->
                          subgraphCreationResult.vajramLogicNodeDefinition().nodeId())
                  .collect(Collectors.toSet()),
              dependencyValues -> accessSpec.merge(dependencyValues.values()));
        }
      }
      return new SubgraphCreationResult(vajramLogicNodeDefinition);
    } else {
      // TODO implement graph creation for blocking node
    }
    // TODO return the proper value
    return null;
  }

  private String generateRandomSuffix() {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
      stringBuilder.append(CHARS.charAt(random.nextInt(CHARS.length())));
    }
    return stringBuilder.toString();
  }

  private record SubgraphCreationResult(NodeDefinition<?> vajramLogicNodeDefinition) {}

  private record ResolverInputs(NodeDefinition<?> nodeDefinition, List<String> boundFrom) {}
}
