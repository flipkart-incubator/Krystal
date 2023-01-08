package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.vajram.VajramLoader.loadVajramsFromClassPath;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.ResolverDefinition;
import com.flipkart.krystal.krystex.node.IOLogicDefinition;
import com.flipkart.krystal.krystex.node.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.node.MainLogicDecorator;
import com.flipkart.krystal.krystex.node.MainLogicDefinition;
import com.flipkart.krystal.krystex.node.NodeDefinition;
import com.flipkart.krystal.krystex.node.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.krystex.node.ResolverCommand;
import com.flipkart.krystal.krystex.node.ResolverLogicDefinition;
import com.flipkart.krystal.utils.ImmutableMapView;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.MandatoryInputsMissingException;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDefinitionException;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.das.AccessSpecMatchingResult;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.das.VajramIndex;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.exec.VajramExecutableGraph;
import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputResolverDefinition;
import com.flipkart.krystal.vajram.inputs.InputSource;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputModulator;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Getter;

/** The execution graph encompassing all registered vajrams. */
public final class VajramNodeGraph implements VajramExecutableGraph {

  @Getter private final NodeDefinitionRegistry nodeDefinitionRegistry;

  private final DecoratedLogicDefinitionRegistry logicRegistry;

  private final Map<VajramID, VajramDefinition> vajramDefinitions = new LinkedHashMap<>();
  /** These are those call graphs of a vajram where no other vajram depends on this. */
  private final Map<VajramID, NodeId> vajramExecutables = new LinkedHashMap<>();

  private final VajramIndex vajramIndex = new VajramIndex();

  private final Map<VajramID, Supplier<InputModulator<InputValuesAdaptor, InputValuesAdaptor>>>
      inputModulators = new LinkedHashMap<>();

  private VajramNodeGraph() {
    LogicDefinitionRegistry logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.nodeDefinitionRegistry = new NodeDefinitionRegistry(logicDefinitionRegistry);
    this.logicRegistry = new DecoratedLogicDefinitionRegistry(logicDefinitionRegistry);
  }

  public static VajramNodeGraph loadFromClasspath(String... packagePrefixes) {
    return loadFromClasspath(packagePrefixes, ImmutableList.of());
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
      VajramID vajramID,
      Supplier<InputModulator<InputValuesAdaptor, InputValuesAdaptor>> inputModulator) {
    inputModulators.put(vajramID, inputModulator);
    Vajram<?> vajram = vajramDefinitions.get(vajramID).getVajram();
    if (vajram instanceof IOVajram<?> ioVajram) {
      Supplier<MainLogicDecorator<Object>> inputModulationDecoratorSupplier =
          getInputModulationDecoratorSupplier(ioVajram, inputModulator);
      NodeId nodeId = vajramExecutables.get(vajramID);
      if (nodeId != null) {
        nodeDefinitionRegistry
            .logicDefinitionRegistry()
            .getMain(nodeDefinitionRegistry.get(nodeId).mainLogicNode())
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
    return _getVajramExecutionGraph(vajramId);
  }

  private NodeId _getVajramExecutionGraph(VajramID vajramId) {
    NodeId nodeId = vajramExecutables.get(vajramId);
    if (nodeId != null) {
      return nodeId;
    }
    nodeId = new NodeId(vajramId.vajramId());
    vajramExecutables.put(vajramId, nodeId);

    VajramDefinition vajramDefinition = getVajramDefinition(vajramId).orElseThrow();

    InputResolverCreationResult inputResolverCreationResult =
        createNodeLogicsForInputResolvers(vajramDefinition);

    ImmutableMap<String, NodeId> depNameToProviderNode =
        createNodeDefinitionsForDependencies(vajramDefinition);

    MainLogicDefinition<?> vajramLogicMainLogicDefinition = createVajramNodeLogic(vajramDefinition);

    NodeDefinition nodeDefinition =
        nodeDefinitionRegistry.newNodeDefinition(
            nodeId.value(),
            vajramLogicMainLogicDefinition.nodeLogicId(),
            depNameToProviderNode,
            inputResolverCreationResult.resolverDefinitions());
    return nodeDefinition.nodeId();
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
                  ResolverLogicDefinition inputResolverNode =
                      logicRegistry.newResolverLogic(
                          "%s:dep(%s):inputResolver(%s)"
                              .formatted(
                                  vajramId, dependencyName, String.join(",", resolvedInputNames)),
                          sources,
                          inputValues -> {
                            validateMandatory(vajramId, inputValues, requiredInputs);
                            DependencyCommand<Inputs> dependencyCommand =
                                vajram.resolveInputOfDependency(
                                    dependencyName, resolvedInputNames, inputValues);
                            if (dependencyCommand
                                instanceof DependencyCommand.Skip<Inputs> skipCommand) {
                              return ResolverCommand.skip(skipCommand.reason());
                            }
                            return ResolverCommand.multiExecuteWith(
                                dependencyCommand.inputs().stream()
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(toImmutableList()));
                          });
                  return new ResolverDefinition(
                      inputResolverNode.nodeLogicId(), sources, dependencyName, resolvedInputNames);
                })
            .collect(toImmutableList());
    return new InputResolverCreationResult(resolverDefinitions);
  }

  private void validateMandatory(
      VajramID vajramID,
      Inputs nodeInputs,
      ImmutableCollection<VajramInputDefinition> requiredInputs) {
    Iterable<VajramInputDefinition> mandatoryInputs =
        requiredInputs.stream()
                .filter(inputDefinition -> inputDefinition instanceof Input<?>)
                .filter(VajramInputDefinition::isMandatory)
            ::iterator;
    Map<String, Throwable> missingMandatoryValues = new HashMap<>();
    for (VajramInputDefinition mandatoryInput : mandatoryInputs) {
      ValueOrError<?> value = nodeInputs.getInputValue(mandatoryInput.name());
      if (value.error().isPresent() || value.value().isEmpty()) {
        missingMandatoryValues.put(
            mandatoryInput.name(),
            value
                .error()
                .orElse(
                    new NoSuchElementException(
                        "No value present for input %s".formatted(mandatoryInput.name()))));
      }
    }
    if (missingMandatoryValues.isEmpty()) {
      return;
    }
    throw new MandatoryInputsMissingException(vajramID, missingMandatoryValues);
  }

  private MainLogicDefinition<?> createVajramNodeLogic(VajramDefinition vajramDefinition) {
    VajramID vajramId = vajramDefinition.getVajram().getId();
    ImmutableCollection<VajramInputDefinition> inputDefinitions =
        vajramDefinition.getVajram().getInputDefinitions();
    ImmutableSet<String> inputs =
        inputDefinitions.stream().map(VajramInputDefinition::name).collect(toImmutableSet());
    NodeLogicId vajramLogicNodeName = new NodeLogicId("%s:vajramLogic".formatted(vajramId));
    // Step 4: Create and register node for the main vajram logic
    if (vajramDefinition.getVajram() instanceof ComputeVajram<?> computeVajram) {
      return logicRegistry.newComputeLogic(
          vajramLogicNodeName.asString(),
          inputs,
          nodeInputs -> {
            validateMandatory(
                vajramId, nodeInputs, vajramDefinition.getVajram().getInputDefinitions());
            Inputs inputValues = injectFromSession(inputDefinitions, nodeInputs);
            return computeVajram.executeCompute(ImmutableList.of(inputValues)).get(inputValues);
          });
    } else if (vajramDefinition.getVajram() instanceof IOVajram<?> ioVajram) {
      IOLogicDefinition<?> ioNodeDefinition =
          logicRegistry.newIOLogic(
              vajramLogicNodeName,
              inputs,
              dependencyValues -> {
                dependencyValues.forEach(
                    nodeInputs ->
                        validateMandatory(
                            vajramId,
                            nodeInputs,
                            vajramDefinition.getVajram().getInputDefinitions()));
                ImmutableList<Inputs> inputValues =
                    dependencyValues.stream()
                        .map(nodeInputs -> injectFromSession(inputDefinitions, nodeInputs))
                        .collect(toImmutableList());
                return ioVajram.execute(inputValues);
              });
      enableInputModulation(ioNodeDefinition, ioVajram);
      return ioNodeDefinition;
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private <T> void enableInputModulation(
      IOLogicDefinition<T> nodeDefinition, IOVajram<?> ioVajram) {
    Supplier<InputModulator<InputValuesAdaptor, InputValuesAdaptor>> inputModulationDecorator =
        inputModulators.get(ioVajram.getId());
    if (inputModulationDecorator != null) {
      nodeDefinition.registerRequestScopedNodeDecorator(
          getInputModulationDecoratorSupplier(ioVajram, inputModulationDecorator));
    }
  }

  private static <T> Supplier<MainLogicDecorator<T>> getInputModulationDecoratorSupplier(
      IOVajram<?> ioVajram,
      Supplier<InputModulator<InputValuesAdaptor, InputValuesAdaptor>> inputModulationDecorator) {
    @SuppressWarnings("unchecked")
    InputsConverter<InputValuesAdaptor, InputValuesAdaptor> inputsConvertor =
        (InputsConverter<InputValuesAdaptor, InputValuesAdaptor>) ioVajram.getInputsConvertor();
    return () -> new InputModulationDecorator<>(inputModulationDecorator.get(), inputsConvertor);
  }

  private static Inputs injectFromSession(
      ImmutableCollection<VajramInputDefinition> inputDefinitions, Inputs inputs) {
    Map<String, InputValue<?>> newValues = new HashMap<>();
    for (VajramInputDefinition inputDefinition : inputDefinitions) {
      String inputName = inputDefinition.name();
      if (inputDefinition instanceof Input<?> input) {
        if (input.sources().contains(InputSource.CLIENT)) {
          InputValue<?> value = inputs.getInputValue(inputName);
          if (ValueOrError.empty().equals(value)) {
            // Input was not resolved by another vajram. Check if it is resolvable
            // by SESSION
            if (input.sources().contains(InputSource.SESSION)) {
              // TODO handle session provided inputs
            }
          }
        }
      }
    }
    if (!newValues.isEmpty()) {
      inputs.values().forEach(newValues::putIfAbsent);
      return new Inputs(ImmutableMapView.copyOf(newValues));
    } else {
      return inputs;
    }
  }

  private ImmutableMap<String, NodeId> createNodeDefinitionsForDependencies(
      VajramDefinition vajramDefinition) {
    List<Dependency> dependencies = new ArrayList<>();
    for (VajramInputDefinition vajramInputDefinition :
        vajramDefinition.getVajram().getInputDefinitions()) {
      if (vajramInputDefinition instanceof Dependency definition) {
        dependencies.add(definition);
      }
    }
    Map<String, NodeId> depNameToProviderNode = new HashMap<>();
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
