package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.vajram.VajramLoader.loadVajramsFromClassPath;
import static com.flipkart.krystal.vajramexecutor.krystex.Utils.toInputValues;
import static com.flipkart.krystal.vajramexecutor.krystex.Utils.toNodeInputs;
import static com.flipkart.krystal.vajramexecutor.krystex.Utils.toSingleValue;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.krystex.MultiResultFuture;
import com.flipkart.krystal.krystex.ResolverDefinition;
import com.flipkart.krystal.krystex.SingleValue;
import com.flipkart.krystal.krystex.node.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.node.IOLogicDefinition;
import com.flipkart.krystal.krystex.node.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeDecorator;
import com.flipkart.krystal.krystex.node.NodeDefinition;
import com.flipkart.krystal.krystex.node.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeInputs;
import com.flipkart.krystal.krystex.node.NodeLogicDefinition;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.ExecutionContextMap;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.MandatoryInputsMissingException;
import com.flipkart.krystal.vajram.ModulatedExecutionContext;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDefinitionException;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.das.AccessSpecMatchingResult;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.das.VajramIndex;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.exec.VajramExecutableGraph;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputResolverDefinition;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.ResolutionSources;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.inputs.ValueOrError;
import com.flipkart.krystal.vajram.modulation.InputModulator;
import com.flipkart.krystal.vajram.modulation.InputModulator.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

/** The execution graph encompassing all registered vajrams. */
public final class VajramNodeGraph implements VajramExecutableGraph {

  @Getter
  private final NodeDefinitionRegistry nodeDefinitionRegistry =
      new NodeDefinitionRegistry(new LogicDefinitionRegistry());

  private final Map<VajramID, VajramDefinition> vajramDefinitions = new LinkedHashMap<>();
  /** These are those call graphs of a vajram where no other vajram depends on this. */
  private final Map<VajramID, NodeDefinition> vajramExecutables = new LinkedHashMap<>();

  private final VajramIndex vajramIndex = new VajramIndex();

  private final Map<VajramID, Supplier<InputModulator<Object, Object>>> inputModulators =
      new LinkedHashMap<>();

  private VajramNodeGraph() {}

  public static VajramNodeGraph loadFromClasspath(String... packagePrefix) {
    return loadFromClasspath(packagePrefix, ImmutableList.of());
  }

  public static VajramNodeGraph loadFromClasspath(
      String[] packagePrefixes, Iterable<Vajram<?>> vajrams) {
    VajramNodeGraph vajramNodeGraph = new VajramNodeGraph();
    for (String packagePrefix : packagePrefixes) {
      loadVajramsFromClassPath(packagePrefix).forEach(vajramNodeGraph::registerVajram);
    }
    vajrams.forEach(vajramNodeGraph::registerVajram);
    return vajramNodeGraph;
  }

  public void registerInputModulator(
      VajramID vajramID, Supplier<InputModulator<Object, Object>> inputModulator) {
    inputModulators.put(vajramID, inputModulator);
    Vajram<?> vajram = vajramDefinitions.get(vajramID).getVajram();
    if (vajram instanceof IOVajram<?> ioVajram) {
      Supplier<NodeDecorator<Object>> inputModulationDecoratorSupplier =
          getInputModulationDecoratorSupplier(ioVajram, inputModulator);
      NodeDefinition nodeDefinition = vajramExecutables.get(vajramID);
      if (nodeDefinition != null) {
        nodeDefinitionRegistry
            .logicDefinitionRegistry()
            .get(nodeDefinition.logicNode())
            .registerRequestScopedNodeDecorator(inputModulationDecoratorSupplier);
      }
    }
  }

  @Override
  public <C extends ApplicationRequestContext> KrystexVajramExecutor<C> createExecutor(
      C requestContext) {
    return new KrystexVajramExecutor<>(this, requestContext);
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
   * If necessary, creates the nodes for the given vajram and, recursively for its dependencies, and
   * returns the {@link NodeId} of the {@link NodeDefinition} corresponding to this vajram.
   *
   * <p>This method should be called once all necessary vajrams have been registered using the
   * {@link #registerVajram(Vajram)} method. If a dependency of a vajram is not registered before
   * this step, this method will throw an exception.
   *
   * @param vajramId The id of the vajram to execute.
   * @return {@link NodeId} of the {@link NodeDefinition} corresponding to this given vajramId
   */
  NodeId getNodeId(VajramID vajramId) {
    return _getVajramExecutionGraph(vajramId).nodeId();
  }

  @NonNull
  private NodeDefinition _getVajramExecutionGraph(VajramID vajramId) {
    NodeDefinition nodeDefinition = vajramExecutables.get(vajramId);
    if (nodeDefinition != null) {
      return nodeDefinition;
    }
    VajramDefinition vajramDefinition = getVajramDefinition(vajramId).orElseThrow();

    InputResolverCreationResult inputResolverCreationResult =
        createNodeLogicsForInputResolvers(vajramDefinition);

    ImmutableMap<String, NodeDefinition> depNameToSubgraph =
        createNodeDefinitionsForDependencies(vajramDefinition);

    NodeLogicDefinition<?> vajramLogicNodeLogicDefinition = createVajramNodeLogic(vajramDefinition);

    ImmutableMap<String, NodeId> depNameToProviderNode =
        depNameToSubgraph.entrySet().stream()
            .collect(toImmutableMap(Entry::getKey, e -> e.getValue().nodeId()));

    nodeDefinition =
        nodeDefinitionRegistry.newNodeDefinition(
            vajramId.vajramId(),
            vajramLogicNodeLogicDefinition.nodeLogicId(),
            depNameToProviderNode,
            inputResolverCreationResult.resolverDefinitions());
    vajramExecutables.put(vajramId, nodeDefinition);
    return nodeDefinition;
  }

  private InputResolverCreationResult createNodeLogicsForInputResolvers(
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
                  ImmutableCollection<VajramInputDefinition> requiredInputs =
                      vajram.getInputDefinitions().stream()
                          .filter(i -> sources.contains(i.name()))
                          .collect(toImmutableList());
                  ComputeLogicDefinition<?> inputResolverNode =
                      nodeDefinitionRegistry
                          .logicDefinitionRegistry()
                          .newBatchComputeLogic(
                              "%s:dep(%s):inputResolver(%s)"
                                  .formatted(
                                      vajramId,
                                      dependencyName,
                                      String.join(",", resolvedInputNames)),
                              sources,
                              nodeInputs -> {
                                validateMandatory(vajramId, nodeInputs, requiredInputs);
                                return vajram
                                    .resolveInputOfDependency(
                                        dependencyName,
                                        resolvedInputNames,
                                        new ExecutionContextMap(toInputValues(nodeInputs)))
                                    .stream()
                                    .map(Utils::toNodeInputs)
                                    .collect(toImmutableList());
                              });
                  return new ResolverDefinition(
                      inputResolverNode.nodeLogicId(), sources, dependencyName, resolvedInputNames);
                })
            .collect(toImmutableList());
    return new InputResolverCreationResult(resolverDefinitions);
  }

  private void validateMandatory(
      VajramID vajramID,
      NodeInputs nodeInputs,
      ImmutableCollection<VajramInputDefinition> requiredInputs) {
    ImmutableCollection<VajramInputDefinition> mandatoryInputs =
        requiredInputs.stream()
            .filter(VajramInputDefinition::isMandatory)
            .collect(toImmutableList());
    Map<String, Throwable> missingMandatoryValues = new HashMap<>();
    for (VajramInputDefinition mandatoryInput : mandatoryInputs) {
      SingleValue<Object> value = nodeInputs.getValue(mandatoryInput.name());
      if (value.isFailure() || value.value().isEmpty()) {
        missingMandatoryValues.put(
            mandatoryInput.name(),
            value
                .failureReason()
                .orElseGet(
                    () ->
                        new NoSuchElementException(
                            "No value present for input %s".formatted(mandatoryInput.name()))));
      }
    }
    if (missingMandatoryValues.isEmpty()) {
      return;
    }
    throw new MandatoryInputsMissingException(vajramID, missingMandatoryValues);
  }

  private NodeLogicDefinition<?> createVajramNodeLogic(VajramDefinition vajramDefinition) {
    VajramID vajramId = vajramDefinition.getVajram().getId();
    ImmutableCollection<VajramInputDefinition> inputDefinitions =
        vajramDefinition.getVajram().getInputDefinitions();
    Set<String> inputs =
        inputDefinitions.stream().map(VajramInputDefinition::name).collect(Collectors.toSet());
    NodeLogicId vajramLogicNodeName = new NodeLogicId("%s:vajramLogic".formatted(vajramId));
    // Step 4: Create and register node for the main vajram logic
    if (vajramDefinition.getVajram() instanceof ComputeVajram<?> computeVajram) {
      return nodeDefinitionRegistry
          .logicDefinitionRegistry()
          .newBatchComputeLogic(
              vajramLogicNodeName.asString(),
              inputs,
              nodeInputs -> {
                validateMandatory(
                    vajramId, nodeInputs, vajramDefinition.getVajram().getInputDefinitions());
                return ImmutableList.of(
                    computeVajram.executeNonBlocking(
                        createExecutionContext(vajramId, inputDefinitions, nodeInputs)));
              });
    } else if (vajramDefinition.getVajram() instanceof IOVajram<?> ioVajram) {
      //noinspection unchecked
      var inputsConvertor = (InputsConverter<Object, Object, Object>) ioVajram.getInputsConvertor();
      IOLogicDefinition<?> ioNodeDefinition =
          nodeDefinitionRegistry
              .logicDefinitionRegistry()
              .newIOLogic(
                  vajramLogicNodeName,
                  inputs,
                  dependencyValues -> {
                    dependencyValues.forEach(
                        nodeInputs ->
                            validateMandatory(
                                vajramId,
                                nodeInputs,
                                vajramDefinition.getVajram().getInputDefinitions()));
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

  private <T> void enableInputModulation(
      IOLogicDefinition<T> nodeDefinition, IOVajram<?> ioVajram) {
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
    Map<String, ValueOrError<?>> map = new HashMap<>();
    for (VajramInputDefinition inputDefinition : inputDefinitions) {
      String inputName = inputDefinition.name();
      if (inputDefinition instanceof Input<?> input) {
        if (input.resolvableBy().contains(ResolutionSources.REQUEST)) {
          if (dependencyValues.values().get(inputName) == null
              || SingleValue.empty().equals(dependencyValues.getValue(inputName))) {
            // Input was not resolved by another node. Check if it is resolvable
            // by SESSION
            if (input.resolvableBy().contains(ResolutionSources.SESSION)) {
              // TODO handle session provided inputs
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

  private ImmutableMap<String, NodeDefinition> createNodeDefinitionsForDependencies(
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

      depNameToProviderNode.put(dependencyName, _getVajramExecutionGraph(dependencyVajram.getId()));
    }
    return ImmutableMap.copyOf(depNameToProviderNode);
  }

  private record InputResolverCreationResult(
      ImmutableList<ResolverDefinition> resolverDefinitions) {}

  private Optional<VajramDefinition> getVajramDefinition(VajramID vajramId) {
    return Optional.ofNullable(vajramDefinitions.get(vajramId));
  }
}
