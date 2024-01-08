package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.data.ValueOrError.withValue;
import static com.flipkart.krystal.krystex.resolution.ResolverCommand.multiExecuteWith;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.VajramLoader.loadVajramsFromClassPath;
import static com.flipkart.krystal.vajram.inputs.InputSource.CLIENT;
import static com.flipkart.krystal.vajram.inputs.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.skipExecution;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolverUtil.collectDepInputs;
import static com.flipkart.krystal.vajram.inputs.resolution.InputResolverUtil.multiResolve;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.ForkJoinExecutorPool;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.resolution.DependencyResolutionRequest;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverCommand;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverLogicDefinition;
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
import com.flipkart.krystal.vajram.inputs.resolution.InputResolverUtil.ResolutionResult;
import com.flipkart.krystal.vajram.inputs.resolution.ResolutionRequest;
import com.flipkart.krystal.vajram.inputs.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.InputModulatorConfig.ModulatorContext;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.InputInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.InputInjector;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import lombok.Getter;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

/** The execution graph encompassing all registered vajrams. */
public final class VajramKryonGraph implements VajramExecutableGraph {

  @Getter private final KryonDefinitionRegistry kryonDefinitionRegistry;

  private final LogicDefRegistryDecorator logicRegistryDecorator;

  private final Map<VajramID, VajramDefinition> vajramDefinitions = new LinkedHashMap<>();

  /** These are those call graphs of a vajram where no other vajram depends on this. */
  private final Map<VajramID, KryonId> vajramExecutables = new LinkedHashMap<>();

  private final VajramIndex vajramIndex = new VajramIndex();

  /** LogicDecorator Id -> LogicDecoratorConfig */
  private final ImmutableMap<String, MainLogicDecoratorConfig> sessionScopedDecoratorConfigs;

  private final LogicDecorationOrdering logicDecorationOrdering;
  private final MultiLeasePool<? extends ExecutorService> executorPool;
  @NotOnlyInitialized private final InputInjector inputInjector;

  private VajramKryonGraph(
      String[] packagePrefixes,
      ImmutableMap<String, MainLogicDecoratorConfig> sessionScopedDecorators,
      LogicDecorationOrdering logicDecorationOrdering,
      @Nullable InputInjectionProvider inputInjectionProvider,
      double maxParallelismPerCore) {
    this.sessionScopedDecoratorConfigs = sessionScopedDecorators;
    this.logicDecorationOrdering = logicDecorationOrdering;
    this.executorPool = new ForkJoinExecutorPool(maxParallelismPerCore);
    LogicDefinitionRegistry logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.kryonDefinitionRegistry = new KryonDefinitionRegistry(logicDefinitionRegistry);
    this.logicRegistryDecorator = new LogicDefRegistryDecorator(logicDefinitionRegistry);
    for (String packagePrefix : packagePrefixes) {
      loadVajramsFromClassPath(packagePrefix).forEach(this::registerVajram);
    }
    this.inputInjector = new InputInjector(this, inputInjectionProvider);
  }

  public MultiLeasePool<? extends ExecutorService> getExecutorPool() {
    return executorPool;
  }

  @Override
  public <C extends ApplicationRequestContext> KrystexVajramExecutor<C> createExecutor(
      C requestContext) {
    return createExecutor(requestContext, KryonExecutorConfig.builder().debug(false).build());
  }

  public <C extends ApplicationRequestContext> KrystexVajramExecutor<C> createExecutor(
      C requestContext, KryonExecutorConfig krystexConfig) {
    if (logicDecorationOrdering != null
        && LogicDecorationOrdering.none().equals(krystexConfig.logicDecorationOrdering())) {
      krystexConfig =
          krystexConfig.toBuilder().logicDecorationOrdering(logicDecorationOrdering).build();
    }
    return new KrystexVajramExecutor<>(this, requestContext, executorPool, krystexConfig);
  }

  public void registerInputModulators(VajramID vajramID, InputModulatorConfig... inputModulators) {
    KryonId kryonId = getKryonId(vajramID);
    VajramDefinition vajramDefinition = vajramDefinitions.get(vajramID);
    MainLogicDefinition<Object> mainLogicDefinition =
        kryonDefinitionRegistry.get(kryonId).getMainLogicDefinition();
    if (kryonId == null || vajramDefinition == null) {
      throw new IllegalArgumentException("Unable to find vajram with id %s".formatted(vajramID));
    }
    Vajram<?> vajram = vajramDefinition.getVajram();
    List<MainLogicDecoratorConfig> mainLogicDecoratorConfigList = new ArrayList<>();
    for (InputModulatorConfig inputModulatorConfig : inputModulators) {
      Predicate<LogicExecutionContext> biFunction =
          logicExecutionContext -> {
            return vajram.getInputDefinitions().stream()
                    .filter(inputDefinition -> inputDefinition instanceof Input<?>)
                    .map(inputDefinition -> (Input<?>) inputDefinition)
                    .anyMatch(Input::needsModulation)
                && inputModulatorConfig.shouldModulate().test(logicExecutionContext);
          };
      mainLogicDecoratorConfigList.add(
          new MainLogicDecoratorConfig(
              InputModulationDecorator.DECORATOR_TYPE,
              biFunction,
              inputModulatorConfig.instanceIdGenerator(),
              decoratorContext ->
                  inputModulatorConfig
                      .decoratorFactory()
                      .apply(new ModulatorContext(vajram, decoratorContext))));
    }
    mainLogicDefinition.registerRequestScopedDecorator(mainLogicDecoratorConfigList);
  }

  /**
   * Returns a new {@link DependantChain} representing the given strings which are passed in trigger
   * order (from [Start] to immediate dependant.)
   *
   * @param firstVajramId The first vajram/kryon in the DependantChain
   * @param firstDependencyName The dependency name of the first vajram/kryon in the DependencyChain
   * @param subsequentDependencyNames an array of strings representing the dependency names in the
   *     DependantChain in trigger order
   */
  public DependantChain computeDependantChain(
      String firstVajramId, String firstDependencyName, String... subsequentDependencyNames) {
    KryonId firstKryonId = _getVajramExecutionGraph(vajramID(firstVajramId));
    KryonDefinition currentKryon = kryonDefinitionRegistry.get(firstKryonId);
    DependantChain currentDepChain =
        kryonDefinitionRegistry.getDependantChainsStart().extend(firstKryonId, firstDependencyName);
    String previousDepName = firstDependencyName;
    for (String currentDepName : subsequentDependencyNames) {
      KryonId depKryonId = currentKryon.dependencyKryons().get(previousDepName);
      if (depKryonId == null) {
        throw new IllegalStateException(
            "Unable find kryon for dependency %s of kryon %s"
                .formatted(currentDepName, currentKryon));
      }
      currentDepChain = currentDepChain.extend(depKryonId, currentDepName);
      currentKryon = kryonDefinitionRegistry.get(depKryonId);
      previousDepName = currentDepName;
    }
    return currentDepChain;
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
  private void registerVajram(@UnderInitialization VajramKryonGraph this, Vajram vajram) {
    if (vajramDefinitions.containsKey(vajram.getId())) {
      return;
    }
    vajramDefinitions.put(vajram.getId(), new VajramDefinition(vajram));
    vajramIndex.add(vajram);
  }

  /**
   * If necessary, creates the kryons for the given vajram and, recursively for its dependencies,
   * and returns the {@link KryonId} of the {@link KryonDefinition} corresponding to this vajram.
   *
   * <p>This method should be called once all necessary vajrams have been registered using the
   * {@link #registerVajram(Vajram)} method. If a dependency of a vajram is not registered before
   * this step, this method will throw an exception.
   *
   * @param vajramId The id of the vajram to execute.
   * @return {@link KryonId} of the {@link KryonDefinition} corresponding to this given vajramId
   */
  KryonId getKryonId(VajramID vajramId) {
    return _getVajramExecutionGraph(vajramId);
  }

  private KryonId _getVajramExecutionGraph(VajramID vajramId) {
    KryonId kryonId = vajramExecutables.get(vajramId);
    if (kryonId != null) {
      return kryonId;
    } else {
      kryonId = new KryonId(vajramId.vajramId());
    }
    vajramExecutables.put(vajramId, kryonId);

    VajramDefinition vajramDefinition =
        getVajramDefinition(vajramId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "Could not find vajram with id: %s".formatted(vajramId)));

    InputResolverCreationResult inputResolverCreationResult =
        createKryonLogicsForInputResolvers(vajramDefinition);

    ImmutableMap<String, KryonId> depNameToProviderKryon =
        createKryonDefinitionsForDependencies(vajramDefinition);

    MainLogicDefinition<?> vajramLogicMainLogicDefinition =
        createVajramKryonLogic(kryonId, vajramDefinition);

    KryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newKryonDefinition(
            kryonId.value(),
            vajramLogicMainLogicDefinition.kryonLogicId(),
            depNameToProviderKryon,
            inputResolverCreationResult.resolverDefinitions(),
            inputResolverCreationResult.multiResolver());
    return kryonDefinition.kryonId();
  }

  private InputResolverCreationResult createKryonLogicsForInputResolvers(
      VajramDefinition vajramDefinition) {
    Vajram<?> vajram = vajramDefinition.getVajram();
    VajramID vajramId = vajram.getId();
    ImmutableCollection<VajramInputDefinition> inputDefinitions = vajram.getInputDefinitions();

    // Create kryon definitions for all input resolvers defined in this vajram
    List<InputResolverDefinition> inputResolvers =
        new ArrayList<>(vajramDefinition.getInputResolverDefinitions());

    ImmutableMap<ResolverDefinition, InputResolverDefinition> resolversByResolverDefs =
        inputResolvers.stream()
            .collect(
                toImmutableMap(
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
                      ResolverLogicDefinition inputResolverLogic =
                          logicRegistryDecorator.newResolverLogic(
                              vajramId.vajramId(),
                              "%s:dep(%s):inputResolver(%s)"
                                  .formatted(
                                      vajramId,
                                      dependencyName,
                                      String.join(",", resolvedInputNames)),
                              sources,
                              inputValues -> {
                                validateMandatory(vajramId, inputValues, requiredInputs);
                                DependencyCommand<Inputs> dependencyCommand;
                                try {
                                  if (inputResolverDefinition
                                      instanceof SimpleInputResolver<?, ?, ?, ?> inputResolver) {
                                    ResolutionResult resolutionResult =
                                        multiResolve(
                                            List.of(
                                                new ResolutionRequest(
                                                    dependencyName, resolvedInputNames)),
                                            ImmutableMap.of(
                                                dependencyName, ImmutableList.of(inputResolver)),
                                            inputValues);
                                    if (resolutionResult
                                        .skippedDependencies()
                                        .containsKey(dependencyName)) {
                                      dependencyCommand =
                                          resolutionResult
                                              .skippedDependencies()
                                              .get(dependencyName);
                                    } else {
                                      dependencyCommand =
                                          toDependencyCommand(
                                              resolutionResult
                                                  .results()
                                                  .values()
                                                  .iterator()
                                                  .next());
                                    }

                                  } else if (inputResolverDefinition
                                      instanceof InputResolver inputResolver) {
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
                                          "Resolver threw exception: %s"
                                              .formatted(getStackTraceAsString(t)));
                                }
                                return toResolverCommand(dependencyCommand);
                              });
                      return new ResolverDefinition(
                          inputResolverLogic.kryonLogicId(),
                          sources,
                          dependencyName,
                          resolvedInputNames);
                    },
                    identity()));
    MultiResolverDefinition multiResolverDefinition =
        logicRegistryDecorator.newMultiResolver(
            vajramId.vajramId(),
            vajramId.vajramId() + ":multiResolver",
            inputDefinitions.stream().map(VajramInputDefinition::name).collect(toImmutableSet()),
            (resolutionRequests, inputs) -> {
              Set<ResolverDefinition> allResolverDefs = new HashSet<>();
              for (DependencyResolutionRequest resolutionRequest : resolutionRequests) {
                Set<ResolverDefinition> resolverDefinitions =
                    resolutionRequest.resolverDefinitions();
                for (ResolverDefinition definition : resolverDefinitions) {
                  allResolverDefs.add(definition);
                }
              }
              Map<String, List<ResolverDefinition>> simpleResolverDefsByDep = new HashMap<>();
              List<ResolverDefinition> complexResolverDefs = new ArrayList<>();
              for (ResolverDefinition resolverDefinition : allResolverDefs) {
                if (resolversByResolverDefs.get(resolverDefinition)
                    instanceof SimpleInputResolver) {
                  simpleResolverDefsByDep
                      .computeIfAbsent(resolverDefinition.dependencyName(), k -> new ArrayList<>())
                      .add(resolverDefinition);
                } else {
                  complexResolverDefs.add(resolverDefinition);
                }
              }
              ResolutionResult simpleResolutions =
                  multiResolve(
                      simpleResolverDefsByDep.entrySet().stream()
                          .map(
                              entry ->
                                  new ResolutionRequest(
                                      entry.getKey(),
                                      entry.getValue().stream()
                                          .map(ResolverDefinition::resolvedInputNames)
                                          .flatMap(Collection::stream)
                                          .collect(toImmutableSet())))
                          .toList(),
                      simpleResolverDefsByDep.entrySet().stream()
                          .collect(
                              toMap(
                                  Entry::getKey,
                                  e ->
                                      e.getValue().stream()
                                          .map(
                                              def ->
                                                  Optional.ofNullable(
                                                          resolversByResolverDefs.get(def))
                                                      .orElseThrow(
                                                          () ->
                                                              new AssertionError(
                                                                  "Could not find resolver for resolver definition. This should not happen")))
                                          .map(ird -> (SimpleInputResolver<?, ?, ?, ?>) ird)
                                          .toList())),
                      inputs);
              Map<String, List<Map<String, @Nullable Object>>> results =
                  simpleResolutions.results();
              Map<String, DependencyCommand<Inputs>> skippedDependencies =
                  simpleResolutions.skippedDependencies();

              Map<String, ResolverCommand> resolverCommands = new LinkedHashMap<>();
              for (ResolverDefinition resolverDef : complexResolverDefs) {
                String dependencyName = resolverDef.dependencyName();
                if (skippedDependencies.containsKey(dependencyName)) {
                  continue;
                }
                ImmutableSet<String> resolvables = resolverDef.resolvedInputNames();
                DependencyCommand<Inputs> command;
                try {
                  if (resolversByResolverDefs.get(resolverDef)
                      instanceof InputResolver inputResolver) {
                    command = inputResolver.resolve(dependencyName, resolvables, inputs);
                  } else {
                    command = vajram.resolveInputOfDependency(dependencyName, resolvables, inputs);
                  }
                } catch (Throwable e) {
                  command =
                      skipExecution(
                          String.format(
                              "Got exception %s while executing the resolver of the dependency %s",
                              e, dependencyName));
                }
                if (command.shouldSkip()) {
                  skippedDependencies.put(dependencyName, command);
                  results.remove(dependencyName);
                } else {
                  //noinspection Convert2Diamond : To handle NullChecker errors.
                  collectDepInputs(
                      results.computeIfAbsent(
                          dependencyName, _k -> new ArrayList<Map<String, @Nullable Object>>()),
                      null,
                      command);
                }
              }
              results.forEach(
                  (key, value) ->
                      resolverCommands.put(key, toResolverCommand(toDependencyCommand(value))));
              skippedDependencies.forEach(
                  (depName, command) -> {
                    resolverCommands.put(depName, ResolverCommand.skip(command.doc()));
                  });
              return ImmutableMap.copyOf(resolverCommands);
            });
    return new InputResolverCreationResult(
        ImmutableList.copyOf(resolversByResolverDefs.keySet()),
        multiResolverDefinition.kryonLogicId());
  }

  private static ResolverCommand toResolverCommand(DependencyCommand<Inputs> dependencyCommand) {
    if (dependencyCommand.shouldSkip()) {
      return ResolverCommand.skip(dependencyCommand.doc());
    }
    return multiExecuteWith(
        dependencyCommand.inputs().stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toImmutableList()));
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

  private MainLogicDefinition<?> createVajramKryonLogic(
      KryonId kryonId, VajramDefinition vajramDefinition) {
    VajramID vajramId = vajramDefinition.getVajram().getId();
    ImmutableCollection<VajramInputDefinition> inputDefinitions =
        vajramDefinition.getVajram().getInputDefinitions();
    ImmutableSet<String> inputNames =
        inputDefinitions.stream()
            .filter(this::isVisibleToKrystex)
            .map(VajramInputDefinition::name)
            .collect(toImmutableSet());
    KryonLogicId vajramLogicKryonName =
        new KryonLogicId(kryonId, "%s:vajramLogic".formatted(vajramId));
    // Step 4: Create and register Kryon for the main vajram logic

    MainLogicDefinition<?> vajramLogic =
        logicRegistryDecorator.newMainLogic(
            vajramDefinition.getVajram() instanceof IOVajram<?>,
            vajramLogicKryonName,
            inputNames,
            inputsList -> {
              List<Inputs> validInputs = new ArrayList<>();
              Map<Inputs, CompletableFuture<@Nullable Object>> failedValidations =
                  new LinkedHashMap<>();
              inputsList.forEach(
                  inputs -> {
                    try {
                      validateMandatory(vajramId, inputs, inputDefinitions);
                      validInputs.add(inputs);
                    } catch (Throwable e) {
                      failedValidations.put(inputs, failedFuture(e));
                    }
                  });
              @SuppressWarnings("unchecked")
              Vajram<Object> vajram = (Vajram<Object>) vajramDefinition.getVajram();
              ImmutableMap<Inputs, CompletableFuture<@Nullable Object>> validResults =
                  vajram.execute(ImmutableList.copyOf(validInputs));

              ImmutableMap<Inputs, CompletableFuture<@Nullable Object>> result =
                  ImmutableMap.<Inputs, CompletableFuture<@Nullable Object>>builder()
                      .putAll(validResults)
                      .putAll(failedValidations)
                      .build();
              return result;
            },
            vajramDefinition.getMainLogicTags());
    registerInputInjector(vajramLogic, vajramDefinition.getVajram());
    sessionScopedDecoratorConfigs
        .values()
        .forEach(vajramLogic::registerSessionScopedLogicDecorator);
    return vajramLogic;
  }

  private boolean isVisibleToKrystex(VajramInputDefinition vajramInputDefinition) {
    if (vajramInputDefinition instanceof Input<?> input) {
      return input.sources().contains(CLIENT);
    }
    return true;
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
            logicExecutionContext -> logicExecutionContext.kryonId().value(),
            decoratorContext -> inputInjector));
  }

  private static DependencyCommand<Inputs> toDependencyCommand(
      List<Map<String, @Nullable Object>> depInputs) {
    DependencyCommand<Inputs> dependencyCommand;
    if (depInputs.isEmpty()) {
      dependencyCommand = executeFanoutWith(ImmutableList.of());
    } else if (depInputs.size() == 1) {
      Map<String, InputValue<Object>> collect =
          depInputs.get(0).entrySet().stream()
              .collect(toMap(Entry::getKey, e -> withValue(e.getValue())));
      dependencyCommand = executeWith(new Inputs(collect));
    } else {
      List<Inputs> inputsList = new ArrayList<>();
      for (Map<String, @Nullable Object> depInput : depInputs) {
        inputsList.add(
            new Inputs(
                depInput.entrySet().stream()
                    .collect(toMap(Entry::getKey, e -> withValue(e.getValue())))));
      }
      dependencyCommand = executeFanoutWith(inputsList);
    }
    return dependencyCommand;
  }

  private ImmutableMap<String, KryonId> createKryonDefinitionsForDependencies(
      VajramDefinition vajramDefinition) {
    List<Dependency> dependencies = new ArrayList<>();
    for (VajramInputDefinition vajramInputDefinition :
        vajramDefinition.getVajram().getInputDefinitions()) {
      if (vajramInputDefinition instanceof Dependency definition) {
        dependencies.add(definition);
      }
    }
    Map<String, KryonId> depNameToProviderKryon = new HashMap<>();
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

      depNameToProviderKryon.put(
          dependencyName, _getVajramExecutionGraph(dependencyVajram.getId()));
    }
    return ImmutableMap.copyOf(depNameToProviderKryon);
  }

  private record InputResolverCreationResult(
      ImmutableList<ResolverDefinition> resolverDefinitions, KryonLogicId multiResolver) {}

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
    private LogicDecorationOrdering logicDecorationOrdering =
        new LogicDecorationOrdering(ImmutableSet.of());
    private @Nullable InputInjectionProvider inputInjectionProvider;
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

    public Builder logicDecorationOrdering(LogicDecorationOrdering logicDecorationOrdering) {
      this.logicDecorationOrdering = logicDecorationOrdering;
      return this;
    }

    public Builder injectInputsWith(InputInjectionProvider inputInjectionProvider) {
      this.inputInjectionProvider = inputInjectionProvider;
      return this;
    }

    public VajramKryonGraph build() {
      return new VajramKryonGraph(
          packagePrefixes.toArray(String[]::new),
          ImmutableMap.copyOf(sessionScopedDecoratorConfigs),
          logicDecorationOrdering,
          inputInjectionProvider,
          maxParallelismPerCore);
    }
  }
}
