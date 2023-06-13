package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.vajram.VajramLoader.loadVajramsFromClassPath;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.skipExecution;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.ForkJoinExecutorPool;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.ResolverCommand;
import com.flipkart.krystal.krystex.ResolverDefinition;
import com.flipkart.krystal.krystex.ResolverLogicDefinition;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.node.NodeDefinition;
import com.flipkart.krystal.krystex.node.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.utils.MultiLeasePool;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
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
import com.flipkart.krystal.vajram.inputs.InputSource;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolver;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolverDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.InputModulatorConfig.ModulatorContext;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.InputInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.InputInjector;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import lombok.Getter;

/** The execution graph encompassing all registered vajrams. */
public final class VajramNodeGraph implements VajramExecutableGraph {

  @Getter private final NodeDefinitionRegistry nodeDefinitionRegistry;

  private final LogicDefRegistryDecorator logicRegistryDecorator;

  private final Map<VajramID, VajramDefinition> vajramDefinitions = new LinkedHashMap<>();
  /** These are those call graphs of a vajram where no other vajram depends on this. */
  private final Map<VajramID, NodeId> vajramExecutables = new LinkedHashMap<>();

  private final VajramIndex vajramIndex = new VajramIndex();

  private final ImmutableMap<VajramID, List<InputModulatorConfig>> inputModulatorConfigs;

  /** LogicDecorator Id -> LogicDecoratorConfig */
  private final ImmutableMap<String, MainLogicDecoratorConfig> sessionScopedDecoratorConfigs;

  private final LogicDecorationOrdering logicDecorationOrdering;
  private final MultiLeasePool<? extends ExecutorService> executorPool;
  private final InputInjector inputInjector;

  private VajramNodeGraph(
      String[] packagePrefixes,
      ImmutableMap<VajramID, List<InputModulatorConfig>> inputModulatorConfigs,
      ImmutableMap<String, MainLogicDecoratorConfig> sessionScopedDecorators,
      LogicDecorationOrdering logicDecorationOrdering,
      InputInjectionProvider inputInjectionProvider,
      double maxParallelismPerCore) {
    this.inputModulatorConfigs = inputModulatorConfigs;
    this.sessionScopedDecoratorConfigs = sessionScopedDecorators;
    this.logicDecorationOrdering = logicDecorationOrdering;
    this.executorPool = new ForkJoinExecutorPool(maxParallelismPerCore);
    LogicDefinitionRegistry logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.nodeDefinitionRegistry = new NodeDefinitionRegistry(logicDefinitionRegistry);
    this.logicRegistryDecorator = new LogicDefRegistryDecorator(logicDefinitionRegistry);
    for (String packagePrefix : packagePrefixes) {
      loadVajramsFromClassPath(packagePrefix).forEach(this::registerVajram);
    }
    inputInjector = new InputInjector(this, inputInjectionProvider);
  }

  public MultiLeasePool<? extends ExecutorService> getExecutorPool() {
    return executorPool;
  }

  @Override
  public <C extends ApplicationRequestContext> KrystexVajramExecutor<C> createExecutor(
      C requestContext) {
    return createExecutor(requestContext, ImmutableMap.of());
  }

  public <C extends ApplicationRequestContext> KrystexVajramExecutor<C> createExecutor(
      C requestContext,
      Map<String, List<MainLogicDecoratorConfig>> requestScopedLogicDecoratorConfigs) {
    return new KrystexVajramExecutor<>(
        this,
        logicDecorationOrdering,
        executorPool,
        requestContext,
        requestScopedLogicDecoratorConfigs);
  }

  @Override
  public void close() {
    executorPool.close();
  }

  /**
   * Registers vajrams that need to be executed at a later point. This is a necessary step for
   * vajram execution.
   *
   * @param vajram The vajram to be registered for future execution.
   */
  private void registerVajram(Vajram vajram) {
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
    } else {
      nodeId = new NodeId(vajramId.vajramId());
    }
    vajramExecutables.put(vajramId, nodeId);

    VajramDefinition vajramDefinition =
        getVajramDefinition(vajramId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "Could not find vajram with id: %s".formatted(vajramId)));

    InputResolverCreationResult inputResolverCreationResult =
        createNodeLogicsForInputResolvers(vajramDefinition);

    ImmutableMap<String, NodeId> depNameToProviderNode =
        createNodeDefinitionsForDependencies(vajramDefinition);

    MainLogicDefinition<?> vajramLogicMainLogicDefinition =
        createVajramNodeLogic(nodeId, vajramDefinition);

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
    ImmutableCollection<VajramInputDefinition> inputDefinitions = vajram.getInputDefinitions();

    // Create node definitions for all input resolvers defined in this vajram
    List<InputResolverDefinition> inputResolvers =
        new ArrayList<>(vajramDefinition.getInputResolverDefinitions());

    ImmutableList<ResolverDefinition> resolverDefinitions =
        inputResolvers.stream()
            .map(
                inputResolverDefinition -> {
                  String dependencyName =
                      inputResolverDefinition.resolutionTarget().dependencyName();
                  ImmutableSet<String> resolvedInputNames =
                      inputResolverDefinition.resolutionTarget().inputNames();
                  ImmutableSet<String> sources = inputResolverDefinition.sources();
                  ImmutableCollection<VajramInputDefinition> requiredInputs =
                      inputDefinitions.stream()
                          .filter(def -> sources.contains(def.name()))
                          .collect(toImmutableList());
                  ResolverLogicDefinition inputResolverNode =
                      logicRegistryDecorator.newResolverLogic(
                          vajramId.vajramId(),
                          "%s:dep(%s):inputResolver(%s)"
                              .formatted(
                                  vajramId, dependencyName, String.join(",", resolvedInputNames)),
                          sources,
                          inputValues -> {
                            validateMandatory(vajramId, inputValues, requiredInputs);
                            DependencyCommand<Inputs> dependencyCommand;
                            try {
                              if (inputResolverDefinition instanceof InputResolver inputResolver) {
                                dependencyCommand =
                                    inputResolver.resolve(
                                        dependencyName, resolvedInputNames, inputValues);
                              } else {
                                dependencyCommand =
                                    vajram.resolveInputOfDependency(
                                        dependencyName, resolvedInputNames, inputValues);
                              }
                            } catch (Throwable t) {
                              dependencyCommand =
                                  skipExecution(
                                      "Resolver threw exception: %s".formatted(getStackTraceAsString(t)));
                            }

                            if (dependencyCommand.shouldSkip()) {
                              return ResolverCommand.skip(dependencyCommand.doc());
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
      VajramID vajramID, Inputs inputs, ImmutableCollection<VajramInputDefinition> requiredInputs) {
    Iterable<VajramInputDefinition> mandatoryInputs =
        requiredInputs.stream()
                .filter(inputDefinition -> inputDefinition instanceof Input<?>)
                .filter(VajramInputDefinition::isMandatory)
            ::iterator;
    Map<String, Throwable> missingMandatoryValues = new HashMap<>();
    for (VajramInputDefinition mandatoryInput : mandatoryInputs) {
      ValueOrError<?> value = inputs.getInputValue(mandatoryInput.name());
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

  private MainLogicDefinition<?> createVajramNodeLogic(
      NodeId nodeId, VajramDefinition vajramDefinition) {
    VajramID vajramId = vajramDefinition.getVajram().getId();
    ImmutableCollection<VajramInputDefinition> inputDefinitions =
        vajramDefinition.getVajram().getInputDefinitions();
    ImmutableSet<String> inputNames =
        inputDefinitions.stream().map(VajramInputDefinition::name).collect(toImmutableSet());
    MainLogicDefinition<?> vajramLogic;
    NodeLogicId vajramLogicNodeName = new NodeLogicId(nodeId, "%s:vajramLogic".formatted(vajramId));
    // Step 4: Create and register node for the main vajram logic
    vajramLogic =
        logicRegistryDecorator.newMainLogic(
            vajramDefinition.getVajram() instanceof IOVajram<?>,
            vajramLogicNodeName,
            inputNames,
            inputsList -> {
              inputsList.forEach(inputs -> validateMandatory(vajramId, inputs, inputDefinitions));
              return vajramDefinition.getVajram().execute(inputsList);
            },
            vajramDefinition.getMainLogicTags());
    enableInputModulation(vajramLogic, vajramDefinition.getVajram());
    registerInputInjector(vajramLogic, vajramDefinition.getVajram());
    sessionScopedDecoratorConfigs
        .values()
        .forEach(vajramLogic::registerSessionScopedLogicDecorator);
    return vajramLogic;
  }

  private <T> void enableInputModulation(
      MainLogicDefinition<T> logicDefinition, Vajram<?> ioVajram) {

    List<InputModulatorConfig> inputModulatorConfigList =
        inputModulatorConfigs.get(ioVajram.getId());
    if (inputModulatorConfigList != null) {
      List<MainLogicDecoratorConfig> mainLogicDecoratorConfigList = new ArrayList<>();
      for (InputModulatorConfig inputModulatorConfig : inputModulatorConfigList) {
        Predicate<LogicExecutionContext> biFunction =
            (nodeExecutionContext) -> {
              return ioVajram.getInputDefinitions().stream()
                      .filter(inputDefinition -> inputDefinition instanceof Input<?>)
                      .map(inputDefinition -> (Input<?>) inputDefinition)
                      .anyMatch(Input::needsModulation)
                  && inputModulatorConfig.shouldModulate().test(nodeExecutionContext);
            };
        mainLogicDecoratorConfigList.add(
            new MainLogicDecoratorConfig(
                InputModulationDecorator.DECORATOR_TYPE,
                biFunction,
                inputModulatorConfig.instanceIdGenerator(),
                decoratorContext ->
                    inputModulatorConfig
                        .decoratorFactory()
                        .apply(new ModulatorContext(ioVajram, decoratorContext))));
      }
      logicDefinition.registerRequestScopedDecorator(mainLogicDecoratorConfigList);
    }
  }

  private <T> void registerInputInjector(MainLogicDefinition<T> logicDefinition, Vajram<?> vajram) {
    logicDefinition.registerSessionScopedLogicDecorator(
        new MainLogicDecoratorConfig(
            InputInjector.DECORATOR_TYPE,
            logicExecutionContext ->
                vajram.getInputDefinitions().stream()
                    .filter(inputDefinition -> inputDefinition instanceof Input<?>)
                    .map(inputDefinition -> ((Input<?>) inputDefinition))
                    .anyMatch(
                        input ->
                            input.sources() != null
                                && input.sources().contains(InputSource.SESSION)),
            logicExecutionContext -> logicExecutionContext.nodeId().value(),
            decoratorContext -> inputInjector));
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
        throw new UnsupportedOperationException("");
      }
      Vajram dependencyVajram = dependencyVajrams.values().iterator().next();

      depNameToProviderNode.put(dependencyName, _getVajramExecutionGraph(dependencyVajram.getId()));
    }
    return ImmutableMap.copyOf(depNameToProviderNode);
  }

  private record InputResolverCreationResult(
      ImmutableList<ResolverDefinition> resolverDefinitions) {}

  public Optional<VajramDefinition> getVajramDefinition(VajramID vajramId) {
    return Optional.ofNullable(vajramDefinitions.get(vajramId));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final Set<String> packagePrefixes = new LinkedHashSet<>();
    private final Map<String, MainLogicDecoratorConfig> sessionScopedDecoratorConfigs =
        new HashMap<>();
    private final Map<VajramID, List<InputModulatorConfig>> inputModulators = new LinkedHashMap<>();
    private LogicDecorationOrdering logicDecorationOrdering =
        new LogicDecorationOrdering(ImmutableSet.of());
    private InputInjectionProvider inputInjectionProvider;
    private double maxParallelismPerCore = 1;

    public Builder loadFromPackage(String packagePrefix) {
      packagePrefixes.add(packagePrefix);
      return this;
    }

    public Builder decorateVajramLogicForSession(MainLogicDecoratorConfig logicDecoratorConfig) {
      if (sessionScopedDecoratorConfigs.putIfAbsent(
              logicDecoratorConfig.decoratorType(), logicDecoratorConfig)
          != null) {
        throw new IllegalArgumentException(
            "Cannot have two decorator configs for same decorator type : %s"
                .formatted(logicDecoratorConfig.decoratorType()));
      }
      return this;
    }

    public Builder maxParallelismPerCore(double maxParallelismPerCore) {
      this.maxParallelismPerCore = maxParallelismPerCore;
      return this;
    }

    public Builder registerInputModulator(VajramID vajramID, InputModulatorConfig inputModulator) {
      if (!inputModulators.containsKey(vajramID)) {
        inputModulators.put(vajramID, new ArrayList<>());
      }
      inputModulators.get(vajramID).add(inputModulator);
      return this;
    }

    public Builder logicDecorationOrdering(LogicDecorationOrdering logicDecorationOrdering) {
      this.logicDecorationOrdering = logicDecorationOrdering;
      return this;
    }

    public Builder injectInputsWith(InputInjectionProvider inputInjectionProvider) {
      this.inputInjectionProvider = inputInjectionProvider;
      return this;
    }

    public VajramNodeGraph build() {
      return new VajramNodeGraph(
          packagePrefixes.toArray(String[]::new),
          ImmutableMap.copyOf(inputModulators),
          ImmutableMap.copyOf(sessionScopedDecoratorConfigs),
          logicDecorationOrdering,
          inputInjectionProvider,
          maxParallelismPerCore);
    }
  }
}
