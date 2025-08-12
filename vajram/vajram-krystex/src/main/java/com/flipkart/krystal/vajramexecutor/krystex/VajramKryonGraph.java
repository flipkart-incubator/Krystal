package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.data.Errable.withValue;
import static com.flipkart.krystal.krystex.resolution.ResolverCommand.multiExecuteWith;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.VajramLoader.loadVajramsFromClassPath;
import static com.flipkart.krystal.vajram.facets.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.collectDepInputs;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.handleResolverException;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.multiResolve;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.resolution.DependencyResolutionRequest;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverCommand;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverLogicDefinition;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.MandatoryFacetsMissingException;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDefinitionException;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.das.AccessSpecMatchingResult;
import com.flipkart.krystal.vajram.das.DataAccessSpec;
import com.flipkart.krystal.vajram.das.VajramIndex;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.exec.VajramExecutableGraph;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.DependencyDef;
import com.flipkart.krystal.vajram.facets.InputDef;
import com.flipkart.krystal.vajram.facets.VajramFacetDefinition;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.resolution.InputResolverDefinition;
import com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.ResolutionResult;
import com.flipkart.krystal.vajram.facets.resolution.ResolutionRequest;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.InputBatcherConfig.BatcherContext;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.Nullable;

/** The execution graph encompassing all registered vajrams. */
@Accessors(fluent = true)
public final class VajramKryonGraph implements VajramExecutableGraph<KrystexVajramExecutorConfig> {

  @Getter private final KryonDefinitionRegistry kryonDefinitionRegistry;

  private final LogicDefRegistryDecorator logicRegistryDecorator;

  private final Map<VajramID, VajramDefinition> vajramDefinitions = new LinkedHashMap<>();
  private final ConcurrentHashMap<Class<? extends Vajram<?>>, VajramDefinition> vajramDataByClass =
      new ConcurrentHashMap<>();

  /** These are those call graphs of a vajram where no other vajram depends on this. */
  private final Map<VajramID, KryonId> vajramExecutables = new LinkedHashMap<>();

  private final VajramIndex vajramIndex = new VajramIndex();

  /** LogicDecorator Id -> LogicDecoratorConfig */
  private final ImmutableMap<String, OutputLogicDecoratorConfig> sessionScopedDecoratorConfigs;

  @Builder
  private VajramKryonGraph(
      LinkedHashSet<String> packagePrefixes,
      Map<String, OutputLogicDecoratorConfig> sessionScopedDecoratorConfigs) {
    this.sessionScopedDecoratorConfigs = ImmutableMap.copyOf(sessionScopedDecoratorConfigs);
    LogicDefinitionRegistry logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.kryonDefinitionRegistry = new KryonDefinitionRegistry(logicDefinitionRegistry);
    this.logicRegistryDecorator = new LogicDefRegistryDecorator(logicDefinitionRegistry);
    for (String packagePrefix : packagePrefixes) {
      loadVajramsFromClassPath(packagePrefix).forEach(this::registerVajram);
    }
  }

  @Override
  public KrystexVajramExecutor createExecutor(KrystexVajramExecutorConfig vajramExecConfig) {
    return KrystexVajramExecutor.builder()
        .vajramKryonGraph(this)
        .executorConfig(vajramExecConfig)
        .build();
  }

  public ImmutableMap<VajramID, VajramDefinition> vajramDefinitions() {
    return ImmutableMap.copyOf(vajramDefinitions);
  }

  public void registerInputBatchers(VajramID vajramID, InputBatcherConfig... inputBatcherConfigs) {
    KryonId kryonId = getKryonId(vajramID);
    VajramDefinition vajramDefinition = vajramDefinitions.get(vajramID);
    OutputLogicDefinition<Object> outputLogicDefinition =
        kryonDefinitionRegistry.get(kryonId).getOutputLogicDefinition();
    if (kryonId == null || vajramDefinition == null) {
      throw new IllegalArgumentException("Unable to find vajram with id %s".formatted(vajramID));
    }
    Vajram<?> vajram = vajramDefinition.vajram();
    List<OutputLogicDecoratorConfig> outputLogicDecoratorConfigList = new ArrayList<>();
    for (InputBatcherConfig inputBatcherConfig : inputBatcherConfigs) {
      Predicate<LogicExecutionContext> biFunction =
          logicExecutionContext -> {
            for (VajramFacetDefinition facetDefinition : vajram.getFacetDefinitions()) {
              if (facetDefinition instanceof InputDef<?> definition) {
                if (definition.isBatched()) {
                  return inputBatcherConfig.shouldBatch().test(logicExecutionContext);
                }
              }
            }
            return false;
          };
      outputLogicDecoratorConfigList.add(
          new OutputLogicDecoratorConfig(
              InputBatchingDecorator.DECORATOR_TYPE,
              biFunction,
              inputBatcherConfig.instanceIdGenerator(),
              decoratorContext ->
                  inputBatcherConfig
                      .decoratorFactory()
                      .apply(new BatcherContext(vajram, decoratorContext)),
              true));
    }
    outputLogicDefinition.registerRequestScopedDecorator(outputLogicDecoratorConfigList);
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
  public void close() {}

  /**
   * Registers vajrams that need to be executed at a later point. This is a necessary step for
   * vajram execution.
   *
   * @param vajram The vajram to be registered for future execution.
   */
  private void registerVajram(Vajram vajram) {
    VajramDefinition vajramDefinition = new VajramDefinition(vajram);
    VajramID vajramID = vajramDefinition.vajramId();
    if (vajramDefinitions.containsKey(vajramID)) {
      return;
    }
    vajramDefinitions.put(vajramID, vajramDefinition);
    vajramIndex.add(vajramDefinition);
    vajramDataByClass.putIfAbsent(vajramDefinition.vajramDefClass(), vajramDefinition);
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

    OutputLogicDefinition<?> outputLogicDefinition =
        createKryonOutputLogic(kryonId, vajramDefinition);

    ImmutableSet<String> inputNames =
        vajramDefinition.vajram().getFacetDefinitions().stream()
            .filter(vajramFacetDefinition -> vajramFacetDefinition instanceof InputDef<?>)
            .map(VajramFacetDefinition::name)
            .collect(toImmutableSet());

    KryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newKryonDefinition(
            kryonId.value(),
            inputNames,
            outputLogicDefinition.kryonLogicId(),
            depNameToProviderKryon,
            inputResolverCreationResult.resolverDefinitions(),
            inputResolverCreationResult.multiResolver(),
            vajramDefinition.vajramTags());
    return kryonDefinition.kryonId();
  }

  private InputResolverCreationResult createKryonLogicsForInputResolvers(
      VajramDefinition vajramDefinition) {
    Vajram<?> vajram = vajramDefinition.vajram();
    VajramID vajramId = vajramDefinition.vajramId();
    ImmutableCollection<VajramFacetDefinition> facetDefinitions = vajram.getFacetDefinitions();

    // Create kryon definitions for all input resolvers defined in this vajram
    List<InputResolverDefinition> inputResolvers =
        new ArrayList<>(vajramDefinition.inputResolverDefinitions());

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
                      ImmutableCollection<VajramFacetDefinition> requiredInputs =
                          facetDefinitions.stream()
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
                                DependencyCommand<Facets> dependencyCommand;
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
                                  dependencyCommand = handleResolverException(t, false);
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
            facetDefinitions.stream().map(VajramFacetDefinition::name).collect(toImmutableSet()),
            (resolutionRequests, inputs) -> {
              Set<ResolverDefinition> allResolverDefs = new HashSet<>();
              for (DependencyResolutionRequest resolutionRequest : resolutionRequests) {
                Set<ResolverDefinition> resolverDefinitions =
                    resolutionRequest.resolverDefinitions();
                allResolverDefs.addAll(resolverDefinitions);
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
              Map<String, DependencyCommand<Facets>> skippedDependencies =
                  simpleResolutions.skippedDependencies();

              Map<String, ResolverCommand> resolverCommands = new LinkedHashMap<>();
              for (ResolverDefinition resolverDef : complexResolverDefs) {
                String dependencyName = resolverDef.dependencyName();
                if (skippedDependencies.containsKey(dependencyName)) {
                  continue;
                }
                ImmutableSet<String> resolvables = resolverDef.resolvedInputNames();
                DependencyCommand<Facets> command;
                try {
                  if (resolversByResolverDefs.get(resolverDef)
                      instanceof InputResolver inputResolver) {
                    command = inputResolver.resolve(dependencyName, resolvables, inputs);
                  } else {
                    command = vajram.resolveInputOfDependency(dependencyName, resolvables, inputs);
                  }
                } catch (Throwable e) {
                  command =
                      handleResolverException(
                          e,
                          false,
                          "Got exception while executing the resolver of the dependency "
                              + dependencyName);
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

  private static ResolverCommand toResolverCommand(DependencyCommand<Facets> dependencyCommand) {
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
      VajramID vajramID, Facets facets, ImmutableCollection<VajramFacetDefinition> requiredInputs) {
    Iterable<VajramFacetDefinition> mandatoryInputs =
        requiredInputs.stream()
                .filter(facetDefinition -> facetDefinition instanceof InputDef<?>)
                .filter(VajramFacetDefinition::isMandatory)
            ::iterator;
    Map<String, Throwable> missingMandatoryValues = new HashMap<>();
    for (VajramFacetDefinition mandatoryInput : mandatoryInputs) {
      Errable<?> value = facets.getInputValue(mandatoryInput.name());
      if (value.error().isPresent() || value.value().isEmpty()) {
        missingMandatoryValues.put(
            mandatoryInput.name(),
            value
                .error()
                .orElse(
                    new NoSuchElementException(
                        "No value present for input '%s'".formatted(mandatoryInput.name()))));
      }
    }
    if (missingMandatoryValues.isEmpty()) {
      return;
    }
    throw new MandatoryFacetsMissingException(vajramID, missingMandatoryValues);
  }

  private OutputLogicDefinition<?> createKryonOutputLogic(
      KryonId kryonId, VajramDefinition vajramDefinition) {
    VajramID vajramId = vajramDefinition.vajramId();
    ImmutableCollection<VajramFacetDefinition> facetDefinitions =
        vajramDefinition.vajram().getFacetDefinitions();
    ImmutableSet<String> inputNames =
        facetDefinitions.stream().map(VajramFacetDefinition::name).collect(toImmutableSet());
    KryonLogicId outputLogicName = new KryonLogicId(kryonId, "%s:outputLogic".formatted(vajramId));
    // Step 4: Create and register Kryon for the output logic

    OutputLogicDefinition<?> outputLogic =
        logicRegistryDecorator.newOutputLogic(
            vajramDefinition.vajram() instanceof IOVajram<?>,
            outputLogicName,
            inputNames,
            inputsList -> {
              List<Facets> validInputs = new ArrayList<>();
              Map<Facets, CompletableFuture<@Nullable Object>> failedValidations =
                  new LinkedHashMap<>();
              inputsList.forEach(
                  inputs -> {
                    try {
                      validateMandatory(vajramId, inputs, facetDefinitions);
                      validInputs.add(inputs);
                    } catch (Throwable e) {
                      failedValidations.put(inputs, failedFuture(e));
                    }
                  });
              @SuppressWarnings("unchecked")
              Vajram<Object> vajram = (Vajram<Object>) vajramDefinition.vajram();
              ImmutableMap<Facets, CompletableFuture<@Nullable Object>> validResults;
              try {
                validResults = vajram.execute(ImmutableList.copyOf(validInputs));
              } catch (Throwable e) {
                return validInputs.stream()
                    .collect(toImmutableMap(identity(), i -> failedFuture(e)));
              }

              return ImmutableMap.<Facets, CompletableFuture<@Nullable Object>>builder()
                  .putAll(validResults)
                  .putAll(failedValidations)
                  .build();
            },
            vajramDefinition.outputLogicTags());
    sessionScopedDecoratorConfigs
        .values()
        .forEach(outputLogic::registerSessionScopedLogicDecorator);
    return outputLogic;
  }

  private static DependencyCommand<Facets> toDependencyCommand(
      List<Map<String, @Nullable Object>> depInputs) {
    DependencyCommand<Facets> dependencyCommand;
    if (depInputs.isEmpty()) {
      dependencyCommand = executeFanoutWith(ImmutableList.of());
    } else if (depInputs.size() == 1) {
      Map<String, FacetValue<Object>> collect =
          depInputs.get(0).entrySet().stream()
              .collect(toMap(Entry::getKey, e -> withValue(e.getValue())));
      dependencyCommand = executeWith(new Facets(collect));
    } else {
      List<Facets> facetsList = new ArrayList<>();
      for (Map<String, @Nullable Object> depInput : depInputs) {
        facetsList.add(
            new Facets(
                depInput.entrySet().stream()
                    .collect(toMap(Entry::getKey, e -> withValue(e.getValue())))));
      }
      dependencyCommand = executeFanoutWith(facetsList);
    }
    return dependencyCommand;
  }

  private ImmutableMap<String, KryonId> createKryonDefinitionsForDependencies(
      VajramDefinition vajramDefinition) {
    List<DependencyDef<?>> dependencies = new ArrayList<>();
    for (VajramFacetDefinition vajramFacetDefinition :
        vajramDefinition.vajram().getFacetDefinitions()) {
      if (vajramFacetDefinition instanceof DependencyDef<?> definition) {
        dependencies.add(definition);
      }
    }
    Map<String, KryonId> depNameToProviderKryon = new HashMap<>();
    // Create and register sub graphs for dependencies of this vajram
    for (DependencyDef<?> dependencyDef : dependencies) {
      var accessSpec = dependencyDef.dataAccessSpec();
      String dependencyName = dependencyDef.name();
      AccessSpecMatchingResult<DataAccessSpec> accessSpecMatchingResult =
          vajramIndex.getVajrams(accessSpec);
      if (accessSpecMatchingResult.hasUnsuccessfulMatches()) {
        throw new VajramDefinitionException(
            "Unable to find vajrams for accessSpecs %s"
                .formatted(accessSpecMatchingResult.unsuccessfulMatches()));
      }
      ImmutableMap<DataAccessSpec, VajramDefinition> dependencyVajrams =
          accessSpecMatchingResult.successfulMatches();
      if (dependencyVajrams.size() > 1) {
        throw new UnsupportedOperationException("");
      }
      VajramDefinition dependencyVajram = dependencyVajrams.values().iterator().next();

      depNameToProviderKryon.put(
          dependencyName, _getVajramExecutionGraph(dependencyVajram.vajramId()));
    }
    return ImmutableMap.copyOf(depNameToProviderKryon);
  }

  private record InputResolverCreationResult(
      ImmutableList<ResolverDefinition> resolverDefinitions, KryonLogicId multiResolver) {}

  public Optional<VajramDefinition> getVajramDefinition(VajramID vajramId) {
    return Optional.ofNullable(vajramDefinitions.get(vajramId));
  }

  public VajramID getVajramId(Class<? extends Vajram<?>> vajramDefClass) {
    VajramDefinition vajramDefinition = vajramDataByClass.get(vajramDefClass);
    if (vajramDefinition == null) {
      throw new IllegalArgumentException(
          "Could not find vajram definition for class %s".formatted(vajramDefClass));
    }
    return vajramDefinition.vajramId();
  }

  public static final class VajramKryonGraphBuilder {
    private final LinkedHashSet<String> packagePrefixes = new LinkedHashSet<>();
    private final Map<String, OutputLogicDecoratorConfig> sessionScopedDecoratorConfigs =
        new HashMap<>();

    public VajramKryonGraphBuilder loadFromPackage(String packagePrefix) {
      packagePrefixes.add(packagePrefix);
      return this;
    }

    public VajramKryonGraphBuilder decorateOutputLogicForSession(
        OutputLogicDecoratorConfig logicDecoratorConfig) {
      if (sessionScopedDecoratorConfigs.putIfAbsent(
              logicDecoratorConfig.decoratorType(), logicDecoratorConfig)
          != null) {
        throw new IllegalArgumentException(
            "Cannot have two decorator configs for same decorator type : %s"
                .formatted(logicDecoratorConfig.decoratorType()));
      }
      return this;
    }

    /**********************************    MAKE PRIVATE   *****************************************/

    // Make this private so that client use loadFromPackage instead.
    private VajramKryonGraphBuilder packagePrefixes(LinkedHashSet<String> packagePrefixes) {
      return this;
    }

    // Make this private so that client use decorateOutputLogicForSession instead.
    private VajramKryonGraphBuilder sessionScopedDecoratorConfigs(
        ImmutableMap<String, OutputLogicDecoratorConfig> sessionScopedDecoratorConfigs) {
      return this;
    }
  }
}
