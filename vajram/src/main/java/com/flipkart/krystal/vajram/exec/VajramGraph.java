package com.flipkart.krystal.vajram.exec;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.krystex.NodeDefinition;
import com.flipkart.krystal.krystex.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.NonBlockingNodeDefinition;
import com.flipkart.krystal.vajram.*;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.exec.VajramDAG.ResolverDefinition;
import com.flipkart.krystal.vajram.inputs.*;
import com.flipkart.krystal.vajram.utils.RandomStringGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * The execution graph encompassing all registered vajrams.
 */
public final class VajramGraph {

  private static final int NODE_ID_SUFFIX_LENGTH = 5;

  @Getter
  private final NodeDefinitionRegistry nodeDefinitionRegistry = new NodeDefinitionRegistry();

  private final Map<String, VajramDefinition> vajramDefinitions = new HashMap<>();
  private final VajramIndex vajramIndex = new VajramIndex();
  private final RandomStringGenerator randomStringGenerator = RandomStringGenerator.instance();

  private VajramGraph() {
  }

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
  public <T> VajramDAG<T> createVajramDAG(String vajramId, Optional<VajramDAG<?>> parentDAG) {
    //noinspection unchecked
    return (VajramDAG<T>) _getVajramExecutionGraph(
        getVajramDefinition(vajramId).orElseThrow().getVajram(), parentDAG, new HashMap<>());
  }

  @NonNull
  private VajramDAG<?> _getVajramExecutionGraph(Vajram<?> vajram,
      Optional<VajramDAG<?>> parentVajramDAG,
      Map<String, Map<String, NodeDefinition<?>>> vajramInputResolverTargets) {
    VajramDAG<?> vajramDAG = new VajramDAG<>(getVajramDefinition(vajram.getId()).orElseThrow(),
        nodeDefinitionRegistry);
//    if (!vajram.isBlockingVajram()) {
      InputResolverCreationResult inputResolverCreationResult = createNodeDefinitionsForInputResolvers(
          vajramDAG, parentVajramDAG, vajramInputResolverTargets);

      SubGraphCreationResult subGraphCreationResult = createSubGraphsForDependencies(vajramDAG,
          vajramInputResolverTargets);
      NodeDefinition<?> vajramLogicNodeDefinition = createVajramLogicNodeDefinition(
          vajramDAG, subGraphCreationResult.depNameToProviderNode);

      updateProviderNodes(vajramDAG.vajramDefinition(), vajramLogicNodeDefinition, parentVajramDAG,
          vajramDAG.nodeDefinitionRegistry(), inputResolverCreationResult.resolverDefinitions(),
          vajramInputResolverTargets, subGraphCreationResult.depNameToProviderNode);

      HashMap<String, String> depNameToProviderNodeId = new HashMap<>();
      subGraphCreationResult.depNameToProviderNode.entrySet().stream().forEach(entry -> {
        depNameToProviderNodeId.put(entry.getKey(), entry.getValue().nodeId());
      });
      return new VajramDAG<>(vajramDAG.vajramDefinition(), vajramLogicNodeDefinition,
          inputResolverCreationResult.resolverDefinitions(),
          inputResolverCreationResult.inputResolverCollector(),
          ImmutableMap.copyOf(depNameToProviderNodeId),
          vajramDAG.nodeDefinitionRegistry());
//    } else {
//      // TODO implement graph creation for blocking node
//      return null;
//    }
  }

  private InputResolverCreationResult createNodeDefinitionsForInputResolvers(VajramDAG<?> vajramDAG,
      Optional<VajramDAG<?>> optionalParentVajramDAG,
      Map<String, Map<String, NodeDefinition<?>>> vajramInputResolverTargets) {

    if (!optionalParentVajramDAG.isPresent()) {
      return new InputResolverCreationResult(ImmutableList.of(), ImmutableList.of());
    }

    VajramDAG<?> parentVajramDAG = optionalParentVajramDAG.get();
    VajramDefinition parentVajramDefination = parentVajramDAG.vajramDefinition();
    NodeDefinitionRegistry nodeDefinitionRegistry = parentVajramDAG.nodeDefinitionRegistry();
    Vajram<?> parentVajram = parentVajramDefination.getVajram();
    String parentVajramId = parentVajram.getId();
    List<NonBlockingNodeDefinition<?>> inputResolverCollector = new LinkedList<>();

    Map<String, Input> inputNameInputMap = new HashMap<>();
    for (VajramInputDefinition inputDefinition : vajramDAG.vajramDefinition().getVajram()
        .getInputDefinitions()) {
      if (inputDefinition instanceof Input input) {
        inputNameInputMap.put(input.name(), input);
      }
    }

    // Names inside current Vajram are unique
    Map<String, InputResolver> parentVajramResolverByName = new HashMap<>();
    Collection<InputResolver> inputResolvers = parentVajramDAG.vajramDefinition()
        .getInputResolvers();
    for (InputResolver inputResolver : inputResolvers) {
      if (inputResolver instanceof ForwardingResolver forwardingResolver) {
        parentVajramResolverByName.put(forwardingResolver.targetInputName(), forwardingResolver);
      }
    }

    Map<String, NodeDefinition<?>> inputToResolverNode = new LinkedHashMap<>();
    ImmutableList<ResolverDefinition> resolverDefinitions = inputNameInputMap.entrySet().stream()
        .map(nameInputEntry -> {
          InputResolver inputResolver = requireNonNull(
              parentVajramResolverByName.get(nameInputEntry.getKey()));
          String dependencyName = inputResolver.resolutionTarget().dependencyName();
          ImmutableSet<String> resolvedInputNames = inputResolver.resolutionTarget().inputNames();
          ImmutableSet<String> parentVajramSources = inputResolver.sources();

          NonBlockingNodeDefinition<?> inputResolverNode = nodeDefinitionRegistry.newNonBlockingBatchNode(
              "v(%s):dep(%s):inputResolver(%s):%s".formatted(parentVajramId, dependencyName,
                  String.join(":", resolvedInputNames), generateNodeSuffix()),
              ImmutableSet.of(),
              ImmutableMap.of(),
              resolvedInputNames,
              dependencyValues -> {
                Map<String, Object> map = new HashMap<>();
                resolvedInputNames.forEach(s -> map.put(s, dependencyValues.get(s)));
                ImmutableList<RequestBuilder<?>> res = null;
                try {
                  res = parentVajram.resolveInputOfDependency(dependencyName, resolvedInputNames,
                      new ExecutionContext(map));
                } catch (Exception e) {
                  String str = e.toString();
                  // Nothing to do, this is triggered for all inputs, which is superset
                  // of dependencyValues
                }
                return res;
              });

          resolvedInputNames.forEach(inputName -> {
            inputToResolverNode.put(inputName, inputResolverNode);
          });

          return new ResolverDefinition(inputResolverNode, parentVajramSources);
        }).collect(toImmutableList());

    if (vajramInputResolverTargets.containsKey(parentVajramId)) {
      Map<String, NodeDefinition<?>> existingMap = vajramInputResolverTargets.get(parentVajramId);
      existingMap.putAll(inputToResolverNode);

      vajramInputResolverTargets.put(parentVajramId, existingMap);
    } else {
      vajramInputResolverTargets.put(parentVajramId, inputToResolverNode);
    }
    return new InputResolverCreationResult(resolverDefinitions,
        ImmutableList.copyOf(inputResolverCollector));
  }

  private NodeDefinition<?> createVajramLogicNodeDefinition(VajramDAG<?> vajramDAG,
      ImmutableMap<String, NodeDefinition<?>> depNameToProviderNode) {
    VajramDefinition vajramDefinition = vajramDAG.vajramDefinition();
    NodeDefinitionRegistry nodeDefinitionRegistry = vajramDAG.nodeDefinitionRegistry();
    String vajramId = vajramDefinition.getVajram().getId();
    List<VajramInputDefinition> inputDefinitions = vajramDefinition.getVajram()
        .getInputDefinitions();
    HashMap<String, String> depNameToProviderNodeId = new HashMap<>();
    depNameToProviderNode.entrySet().stream().forEach(entry -> {
      depNameToProviderNodeId.put(entry.getKey(), entry.getValue().nodeId());
    });

    // Step 4: Create and register node for the main vajram logic
    if (vajramDefinition.getVajram() instanceof NonBlockingVajram<?> nonBlockingVajram) {
      return nodeDefinitionRegistry.newNonBlockingBatchNode(
          "n(v(%s):vajramLogic:%s)".formatted(vajramId, generateNodeSuffix()),
          inputDefinitions.stream().filter(inputDefinition -> inputDefinition instanceof Dependency)
              .map(VajramInputDefinition::name).collect(Collectors.toSet()),
          depNameToProviderNodeId,
          // For vajram, resolvers are added for inputs, so inputs will need to be empty.
          ImmutableSet.of(),
          dependencyValues -> {
            Map<String, Object> map = new HashMap<>();
            for (VajramInputDefinition inputDefinition : inputDefinitions) {
              String inputName = inputDefinition.name();
              if (inputDefinition instanceof Input<?> input) {
                if (input.resolvableBy().contains(ResolutionSources.REQUEST)) {
                  if (dependencyValues.get(inputName) == null || Objects.equals(
                      dependencyValues.get(inputName), Optional.empty())) {
                    // Input was not resolved by another node. Check if it is resolvable
                    // by SESSION
                    if (input.resolvableBy().contains(ResolutionSources.SESSION)) {
                      // TODO handle session provided inputs
                    } else {
                      throw new VajramDefinitionException(
                          "Input: " + input.name() + " of vajram: " + vajramId
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
    } else if (vajramDefinition.getVajram() instanceof BlockingVajram<?> blockingVajram) {
      return nodeDefinitionRegistry.newBlockingBatchNode(
          "n(v(%s):vajramLogic:%s)".formatted(vajramId, generateNodeSuffix()),
          inputDefinitions.stream().filter(inputDefinition -> inputDefinition instanceof Dependency)
              .map(VajramInputDefinition::name).collect(Collectors.toSet()),
          depNameToProviderNodeId,
          // For vajram, resolvers are added for inputs, so inputs will need to be empty.
          ImmutableSet.of(),
          dependencyValues -> {
            Map<String, Object> map = new HashMap<>();
            for (VajramInputDefinition inputDefinition : inputDefinitions) {
              String inputName = inputDefinition.name();
              if (inputDefinition instanceof Input<?> input) {
                if (input.resolvableBy().contains(ResolutionSources.REQUEST)) {
                  if (dependencyValues.get(inputName) == null || Objects.equals(
                      dependencyValues.get(inputName), Optional.empty())) {
                    // Input was not resolved by another node. Check if it is resolvable
                    // by SESSION
                    if (input.resolvableBy().contains(ResolutionSources.SESSION)) {
                      // TODO handle session provided inputs
                    } else {
                      throw new VajramDefinitionException(
                          "Input: " + input.name() + " of vajram: " + vajramId
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
            return blockingVajram.executeBlocking(new ExecutionContext(map));
          });
    }

    // throw
    return null;
  }

  private SubGraphCreationResult createSubGraphsForDependencies(VajramDAG<?> vajramDAG,
      Map<String, Map<String, NodeDefinition<?>>> vajramInputResolverTargets) {
    VajramDefinition vajramDefinition = vajramDAG.vajramDefinition();
    NodeDefinitionRegistry nodeDefinitionRegistry = vajramDAG.nodeDefinitionRegistry();
    String vajramId = vajramDefinition.getVajram().getId();
    List<NonBlockingNodeDefinition<?>> inputResolverCollector = new LinkedList<>();

    List<Dependency> dependencies = new ArrayList<>();
    for (VajramInputDefinition vajramInputDefinition : vajramDefinition.getVajram()
        .getInputDefinitions()) {
      if (vajramInputDefinition instanceof Dependency definition) {
        dependencies.add(definition);
      }
    }
    Map<String, NodeDefinition<?>> depNameToProviderNode = new HashMap<>();
    // Create and register sub graphs for dependencies of this vajram
    for (Dependency dependency : dependencies) {
      var accessSpec = dependency.dataAccessSpec();
      String dependencyName = dependency.name();
      AccessSpecMatchingResult<DataAccessSpec> accessSpecMatchingResult = vajramIndex.getVajrams(
          accessSpec);
      if (accessSpecMatchingResult.hasUnsuccessfulMatches()) {
        throw new VajramDefinitionException("Unable to find vajrams for accessSpecs %s".formatted(
            accessSpecMatchingResult.unsuccessfulMatches()));
      }
      ImmutableMap<DataAccessSpec, Vajram<?>> dependencyVajrams = accessSpecMatchingResult.successfulMatches();
      Map<DataAccessSpec, VajramDAG<?>> dependencySubGraphs = new HashMap<>();

      Map<String, InputResolver> inputResolverMap = new HashMap<>();

      vajramDefinition.getInputResolvers().forEach(resolver -> {
        String inputResolverDepName = resolver.resolutionTarget().dependencyName();
        inputResolverMap.put(inputResolverDepName, resolver);
      });

      Map<String, List<InputResolver>> vajramIDInputResolverMap = new HashMap<>();
      Map<DataAccessSpec, List<InputResolver>> accessSpecInputResolverMap = new HashMap<>();
      for (VajramInputDefinition vajramInputDefinition : vajramDefinition.getVajram()
          .getInputDefinitions()) {
        if (vajramInputDefinition instanceof Dependency vajramDependnecy) {
          if (dependencyVajrams.containsKey(vajramDependnecy.dataAccessSpec())) {
            if (inputResolverMap.containsKey(vajramDependnecy.name())) {
              if (vajramDependnecy.dataAccessSpec() instanceof VajramID vajram) {
                vajramIDInputResolverMap.computeIfAbsent(vajram.vajramId(), k -> new LinkedList<>())
                    .add(inputResolverMap.get(vajramDependnecy.name()));
                accessSpecInputResolverMap.computeIfAbsent(vajramDependnecy.dataAccessSpec(),
                    k -> new LinkedList<>()).add(inputResolverMap.get(vajramDependnecy.name()));
              }
            }
          }
        }
      }

      dependencyVajrams.forEach((dependencySpec, depVajram) -> {
        VajramDAG<?> subGraphDAG = _getVajramExecutionGraph(depVajram, Optional.of(vajramDAG),
            vajramInputResolverTargets);
        inputResolverCollector.addAll(subGraphDAG.inputAdapterNodes());
        dependencySubGraphs.put(dependencySpec, subGraphDAG);
      });

      if (dependencySubGraphs.size() > 1
          // Since this access spec is being powered by multiple vajrams, we will need to merge
          // the responses
          ||
          // Since some vajrams are giving more data than has been requested, we will need
          // to prune the data to prevent unnecessary data from leaking to the logic in this
          // vajram
          accessSpecMatchingResult.needsAdaption()) {
        // Create adaptor node if vajram responses need to be adapted
        String nodeId = "v(%s):dep(%s):n(adaptor):%s".formatted(vajramId, dependencyName,
            generateNodeSuffix());
        NodeDefinition nodeDefinition = nodeDefinitionRegistry.newNonBlockingNode(nodeId,
            ImmutableSet.of(),
            // Set all nodes powering the dependency access spec as dependencies to the adaptor node.
            dependencySubGraphs.entrySet().stream().collect(
                toImmutableMap(e -> e.getKey().toString(),
                    e -> e.getValue().vajramLogicNodeDefinition().nodeId())),
            ImmutableSet.of(),
            dependencyValues -> accessSpec.adapt(dependencyValues.values()));
        depNameToProviderNode.put(dependencyName, nodeDefinition);
      } else {
        depNameToProviderNode.put(dependencyName,
            dependencySubGraphs.values().iterator().next().vajramLogicNodeDefinition());
      }
    }

    return new SubGraphCreationResult(ImmutableMap.copyOf(depNameToProviderNode),
        ImmutableList.copyOf(inputResolverCollector));
  }

  private static void updateProviderNodes(VajramDefinition vajramDefinition,
      NodeDefinition vajramLogicNodeDefinition, Optional<VajramDAG<?>> parentVajramDAG,
      NodeDefinitionRegistry nodeDefinitionRegistry,
      ImmutableList<ResolverDefinition> resolverDefinitions,
      Map<String, Map<String, NodeDefinition<?>>> vajramInputResolverTargets,
      Map<String, NodeDefinition<?>> depNameToProviderNode) {

    Map<String, NodeDefinition<?>> inputToResolver = new HashMap<>();
    if (parentVajramDAG.isPresent()) {
      inputToResolver = vajramInputResolverTargets.get(
          parentVajramDAG.get().vajramDefinition().getVajram().getId());
    }

    // Here node id is the unique, so reverse the mapping
    Map<String, String> nodeIdToinputName = new HashMap<>();
    for (Map.Entry<String, NodeDefinition<?>> entry : inputToResolver.entrySet()) {
      nodeIdToinputName.put(entry.getValue().nodeId(), entry.getKey());
    }

    resolverDefinitions.forEach(resolverDefinition -> {
      resolverDefinition.boundFrom().forEach(from -> {
        String nodeId = resolverDefinition.nodeDefinition().nodeId();
        String inputName = nodeIdToinputName.get(nodeId);
        vajramLogicNodeDefinition.addDependencyProvider(inputName, nodeId);
      });
    });

    String vajramId = vajramDefinition.getVajram().getId();
    if (!vajramInputResolverTargets.containsKey(vajramId)) {
      return;
    }

    Map<String, String> inputResolverTargetToDepName = new HashMap<>();
    for (InputResolver inputResolver : vajramDefinition.getInputResolvers()) {
      if (inputResolver instanceof ForwardingResolver forwardingResolver) {
        inputResolverTargetToDepName.put(forwardingResolver.targetInputName(),
            forwardingResolver.from());
      }
    }

    Map<String, NodeDefinition<?>> inputResolverMap = vajramInputResolverTargets.get(vajramId);
    Map<String, InputResolver> targetInputToParentResolver = new HashMap<>();
    for (InputResolver inputResolver : vajramDefinition.getInputResolvers()) {
      if (inputResolver instanceof ForwardingResolver forwardingResolver) {
        String targetInputName = forwardingResolver.targetInputName();
        if (inputResolverMap.containsKey(targetInputName)) {
          NodeDefinition<?> resolverNode = inputResolverMap.get(targetInputName);

          String depName = inputResolverTargetToDepName.get(targetInputName);
//          if (depNameToProviderNode.containsKey(depName)) {
//            NodeDefinition providerNode = depNameToProviderNode.get(depName);
//            providerNode.addInputAdaptionTarget(depName, targetInputName, resolverNode);
//          } else {
            vajramLogicNodeDefinition.addInputAdaptionTarget(depName, targetInputName,
                resolverNode);
//          }
        }
      }
    }
  }

  private record InputResolverCreationResult(ImmutableList<ResolverDefinition> resolverDefinitions,
                                             ImmutableList<NonBlockingNodeDefinition<?>> inputResolverCollector) {

  }

  private record SubGraphCreationResult(
      ImmutableMap<String, NodeDefinition<?>> depNameToProviderNode,
      ImmutableList<NonBlockingNodeDefinition<?>> inputResolverCollector) {

  }

  private String generateNodeSuffix() {
    return randomStringGenerator.generateRandomString(NODE_ID_SUFFIX_LENGTH);
  }

  private Optional<VajramDefinition> getVajramDefinition(String vajramId) {
    return Optional.ofNullable(vajramDefinitions.get(vajramId));
  }
}
