package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.tags.ElementTags.emptyTags;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.facets.FacetValidation.validateMandatoryFacet;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.handleResolverException;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.toResolverCommand;
import static com.flipkart.krystal.vajram.utils.VajramLoader.loadVajramsFromClassPath;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.facets.BasicFacetInfo;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig.LogicDecoratorContext;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.krystex.resolution.Resolver;
import com.flipkart.krystal.krystex.resolution.ResolverLogic;
import com.flipkart.krystal.vajram.BatchableVajram;
import com.flipkart.krystal.vajram.IOVajram;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.exception.MandatoryFacetMissingException;
import com.flipkart.krystal.vajram.exception.MandatoryFacetsMissingException;
import com.flipkart.krystal.vajram.exception.VajramDefinitionException;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.exec.VajramExecutableGraph;
import com.flipkart.krystal.vajram.facets.resolution.FanoutInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.facets.resolution.One2OneInputResolver;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.utils.VajramLoader;
import com.flipkart.krystal.vajramexecutor.krystex.InputBatcherConfig.BatcherContext;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

/** The execution graph encompassing all registered vajrams. */
public final class VajramKryonGraph implements VajramExecutableGraph<KrystexVajramExecutorConfig> {

  @Getter private final KryonDefinitionRegistry kryonDefinitionRegistry;

  private final LogicDefRegistryDecorator logicRegistryDecorator;

  private final Map<VajramID, VajramDefinition> vajramDefinitions = new ConcurrentHashMap<>();
  private final Map<Class<? extends Vajram<?>>, VajramDefinition> vajramDataByClass =
      new ConcurrentHashMap<>();

  /**
   * Maps every vajramId to its corresponding kryonId all of whose dependencies have also been
   * loaded recursively. The mapped kryon id represents the complete executable sub-graph of the
   * vajram.
   */
  private final Map<VajramID, KryonId> vajramExecutables = new ConcurrentHashMap<>();

  /** LogicDecorator Id -> LogicDecoratorConfig */
  private final ImmutableMap<String, OutputLogicDecoratorConfig> sessionScopedDecoratorConfigs;

  @lombok.Builder
  private VajramKryonGraph(
      Set<String> packagePrefixes,
      Set<Class<? extends Vajram>> classes,
      Map<String, OutputLogicDecoratorConfig> sessionScopedDecoratorConfigs) {
    this.sessionScopedDecoratorConfigs = ImmutableMap.copyOf(sessionScopedDecoratorConfigs);
    LogicDefinitionRegistry logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.kryonDefinitionRegistry = new KryonDefinitionRegistry(logicDefinitionRegistry);
    this.logicRegistryDecorator = new LogicDefRegistryDecorator(logicDefinitionRegistry);
    for (String packagePrefix : packagePrefixes) {
      loadVajramsFromClassPath(packagePrefix).forEach(this::registerVajram);
    }
    for (Class<? extends Vajram> clazz : classes) {
      this.registerVajram(VajramLoader.loadVajramsFromClass(clazz));
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
    if (!(vajram instanceof BatchableVajram<?> batchableVajram)) {
      throw new VajramDefinitionException(
          "Cannot register input Batchers for vajram %s since it is not a BatchableVajram"
              .formatted(vajramID.vajramId()));
    }
    List<OutputLogicDecoratorConfig> outputLogicDecoratorConfigList = new ArrayList<>();
    for (InputBatcherConfig inputBatcherConfig : inputBatcherConfigs) {
      Predicate<LogicExecutionContext> shouldDecorate =
          logicExecutionContext -> {
            BatcherContext batcherContext =
                new BatcherContext(
                    batchableVajram,
                    new LogicDecoratorContext(
                        inputBatcherConfig.instanceIdGenerator().apply(logicExecutionContext),
                        logicExecutionContext));
            return inputBatcherConfig.shouldBatch().test(batcherContext)
                && vajramDefinition.vajramMetadata().isBatched();
          };
      outputLogicDecoratorConfigList.add(
          new OutputLogicDecoratorConfig(
              InputBatchingDecorator.DECORATOR_TYPE,
              shouldDecorate,
              inputBatcherConfig.instanceIdGenerator(),
              decoratorContext ->
                  inputBatcherConfig
                      .decoratorFactory()
                      .apply(new BatcherContext(batchableVajram, decoratorContext))));
    }
    outputLogicDefinition.registerRequestScopedDecorator(outputLogicDecoratorConfigList);
  }

  /**
   * Returns a new {@link DependantChain} representing the given strings which are passed in trigger
   * order (from [Start] to immediate dependant.)
   *
   * @param firstVajramId The first vajram/kryon in the DependantChain
   * @param firstDependencyId The dependency id of the first vajram/kryon in the DependencyChain
   * @param subsequentDependencyIds an array of strings representing the dependency names in the
   *     DependantChain in trigger order
   */
  public DependantChain computeDependantChain(
      String firstVajramId, Dependency firstDependencyId, Dependency... subsequentDependencyIds) {
    KryonId firstKryonId = getKryonId(vajramID(firstVajramId));
    KryonDefinition currentKryon = kryonDefinitionRegistry.get(firstKryonId);
    DependantChain currentDepChain =
        kryonDefinitionRegistry.getDependantChainsStart().extend(firstKryonId, firstDependencyId);
    Facet previousDepName = firstDependencyId;
    for (Dependency currentDepName : subsequentDependencyIds) {
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
  private void registerVajram(Vajram<Object> vajram) {
    VajramDefinition vajramDefinition = new VajramDefinition(vajram);
    VajramID vajramID = vajramDefinition.vajramId();
    if (vajramDefinitions.containsKey(vajramID)) {
      return;
    }
    vajramDefinitions.put(vajramID, vajramDefinition);
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
    KryonId kryonId = vajramExecutables.get(vajramId);
    if (kryonId != null) {
      return kryonId;
    } else {
      return loadKryonSubgraph(vajramId, new LinkedHashMap<>());
    }
  }

  private KryonId loadKryonSubgraph(VajramID vajramId, Map<VajramID, KryonId> loadingInProgress) {
    synchronized (vajramExecutables) {
      KryonId kryonId;
      if ((kryonId = vajramExecutables.get(vajramId)) != null) {
        // This means the subgraph is already loaded.
        return kryonId;
      } else if ((kryonId = loadingInProgress.get(vajramId)) != null) {
        // This means the subgraph is still being loaded, but there is a cyclic dependency. Just
        // return the kryon to prevent infinite recursion.
        return kryonId;
      }
      kryonId = new KryonId(vajramId.vajramId());
      // add to loadingInProgress so that this can be used to prevent infinite recursion in the
      // cases where a vajram depends on itself in a cyclic dependency.
      loadingInProgress.put(vajramId, kryonId);
      VajramDefinition vajramDefinition =
          getVajramDefinition(vajramId)
              .orElseThrow(
                  () ->
                      new NoSuchElementException(
                          "Could not find vajram with id: %s".formatted(vajramId)));
      InputResolverCreationResult inputResolverCreationResult =
          createKryonLogicsForInputResolvers(vajramDefinition);
      ImmutableMap<Dependency, KryonId> depIdToProviderKryon =
          createKryonDefinitionsForDependencies(vajramDefinition, loadingInProgress);
      OutputLogicDefinition<?> outputLogicDefinition =
          createKryonOutputLogic(kryonId, vajramDefinition);
      ImmutableSet<? extends Facet> inputIds = vajramDefinition.facetSpecs();
      LogicDefinition<CreateNewRequest> createNewRequest =
          new LogicDefinition<>(
              new KryonLogicId(kryonId, "%s:newRequest"),
              ImmutableSet.of(),
              emptyTags(),
              vajramDefinition.vajram()::newRequestBuilder);
      KryonDefinition kryonDefinition =
          kryonDefinitionRegistry.newKryonDefinition(
              kryonId.value(),
              inputIds,
              outputLogicDefinition.kryonLogicId(),
              depIdToProviderKryon,
              inputResolverCreationResult.resolversByDefinition(),
              createNewRequest,
              new LogicDefinition<>(
                  new KryonLogicId(kryonId, "%s:facetsFromRequest"),
                  ImmutableSet.of(),
                  emptyTags(),
                  r -> vajramDefinition.vajram().facetsFromRequest(r)),
              vajramDefinition.vajramTags());
      vajramExecutables.put(vajramId, kryonId);
      return kryonDefinition.kryonId();
    }
  }

  private InputResolverCreationResult createKryonLogicsForInputResolvers(
      VajramDefinition vajramDefinition) {
    VajramID vajramId = vajramDefinition.vajramId();
    ImmutableSet<FacetSpec> facetDefinitions = vajramDefinition.facetSpecs();

    // Create kryon definitions for all input resolvers defined in this vajram
    ImmutableMap<ResolverDefinition, InputResolver> inputResolvers =
        vajramDefinition.inputResolvers();

    Map<ResolverDefinition, Resolver> resolverDefinitions = new LinkedHashMap<>();
    inputResolvers.forEach(
        (resolverId, inputResolver) -> {
          ResolverDefinition inputResolverDefinition = inputResolver.definition();
          Facet dependencyId = inputResolverDefinition.target().dependency();
          Facet facet = dependencyId;
          if (facet == null) {
            throw new IllegalStateException();
          }
          String depName = facet.name();

          ImmutableSet<? extends Facet> sources = inputResolver.definition().sources();
          ImmutableCollection<FacetSpec> requiredInputs =
              facetDefinitions.stream().filter(e -> sources.contains(e)).collect(toImmutableList());
          LogicDefinition<ResolverLogic> inputResolverLogic =
              logicRegistryDecorator.newResolverLogic(
                  vajramId.vajramId(),
                  "%s:dep(%s):inputResolver(%s)"
                      .formatted(
                          vajramId,
                          dependencyId,
                          String.join(
                              ",",
                              inputResolver.definition().target().targetInputs().stream()
                                  .map(BasicFacetInfo::name)
                                  .toList())),
                  sources,
                  (depRequests, facets) -> {
                    validateMandatory(vajramId, facets, requiredInputs);
                    ResolverCommand resolverCommand;
                    try {
                      if (inputResolver instanceof One2OneInputResolver singleInputResolver) {
                        resolverCommand = singleInputResolver.resolve(depRequests, facets);
                      } else if (inputResolver instanceof FanoutInputResolver fanoutInputResolver) {
                        if (depRequests.size() != 1) {
                          throw new IllegalStateException(
                              "The vajram "
                                  + vajramId.vajramId()
                                  + " can have at most one fanout resolver");
                        }
                        resolverCommand = fanoutInputResolver.resolve(depRequests.get(0), facets);
                      } else {
                        throw new UnsupportedOperationException(
                            "Unsupported input resolver type: " + inputResolver);
                      }
                    } catch (Throwable t) {
                      resolverCommand =
                          toResolverCommand(
                              handleResolverException(
                                  t,
                                  false,
                                  "Got exception '%s' while executing the resolver of the dependency '%s'"
                                      .formatted(t, depName)));
                    }
                    return resolverCommand;
                  });
          Resolver resolver =
              new Resolver(inputResolverLogic.kryonLogicId(), inputResolver.definition());
          resolverDefinitions.put(resolver.definition(), resolver);
        });
    return new InputResolverCreationResult(ImmutableMap.copyOf(resolverDefinitions));
  }

  private void validateMandatory(
      VajramID vajramID, FacetValues facetValues, ImmutableCollection<FacetSpec> requiredInputs) {
    @SuppressWarnings("StreamToIterable")
    Iterable<FacetSpec> mandatoryFacets =
        requiredInputs.stream().filter(FacetSpec::isMandatory)::iterator;
    Map<String, Throwable> missingMandatoryValues = new HashMap<>();
    for (Facet mandatoryFacet : mandatoryFacets) {
      FacetValue facetValue = mandatoryFacet.getFacetValue(facetValues);
      Errable<?> value;
      if (facetValue instanceof Errable<?> errable) {
        value = errable;
      } else if (facetValue instanceof One2OneDepResponse<?, ?> depResponse) {
        value = depResponse.response();
      } else if (facetValue instanceof FanoutDepResponses<?, ?> fanoutDepResponses) {
        if (fanoutDepResponses.requestResponsePairs().stream()
            .allMatch(reqResp -> reqResp.response().valueOpt().isEmpty())) {
          missingMandatoryValues.put(
              mandatoryFacet.name(),
              new MandatoryFacetMissingException(vajramID.vajramId(), mandatoryFacet.name()));
        }
        continue;
      } else {
        continue;
      }
      Optional<Throwable> error = value.errorOpt();
      if (error.isPresent()) {
        missingMandatoryValues.put(mandatoryFacet.name(), error.get());
      } else {
        try {
          validateMandatoryFacet(
              value.valueOpt().orElse(null), vajramID.vajramId(), mandatoryFacet.name());
        } catch (Throwable e) {
          missingMandatoryValues.put(mandatoryFacet.name(), e);
        }
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
    ImmutableSet<FacetSpec> facetSpecs = vajramDefinition.facetSpecs();
    ImmutableSet<Facet> kryonOutputLogicSources =
        facetSpecs.stream()
            .filter(vajramDefinition.outputLogicSources()::contains)
            .collect(toImmutableSet());
    KryonLogicId outputLogicName = new KryonLogicId(kryonId, "%s:outputLogic".formatted(vajramId));

    // Step 4: Create and register Kryon for the output logic
    OutputLogic<@Nullable Object> outputLogicCode =
        inputsList -> {
          List<FacetValues> validInputs = new ArrayList<>();
          Map<FacetValues, CompletableFuture<@Nullable Object>> failedValidations =
              new LinkedHashMap<>();
          inputsList.forEach(
              inputs -> {
                try {
                  validateMandatory(vajramId, inputs, facetSpecs);
                  validInputs.add(inputs);
                } catch (Throwable e) {
                  failedValidations.put(inputs, failedFuture(e));
                }
              });
          Vajram<Object> vajram = vajramDefinition.vajram();
          ImmutableMap<FacetValues, CompletableFuture<@Nullable Object>> validResults;
          try {
            validResults = vajram.execute(ImmutableList.copyOf(validInputs));
          } catch (Throwable e) {
            return validInputs.stream().collect(toImmutableMap(identity(), i -> failedFuture(e)));
          }

          return ImmutableMap.<FacetValues, CompletableFuture<@Nullable Object>>builder()
              .putAll(validResults)
              .putAll(failedValidations)
              .build();
        };
    OutputLogicDefinition<?> outputLogic =
        logicRegistryDecorator.newOutputLogic(
            vajramDefinition.vajram() instanceof IOVajram<?>,
            outputLogicName,
            kryonOutputLogicSources,
            outputLogicCode,
            vajramDefinition.outputLogicTags());
    sessionScopedDecoratorConfigs
        .values()
        .forEach(outputLogic::registerSessionScopedLogicDecorator);
    return outputLogic;
  }

  private ImmutableMap<Dependency, KryonId> createKryonDefinitionsForDependencies(
      VajramDefinition vajramDefinition, Map<VajramID, KryonId> loadingInProgress) {
    List<DependencySpec> dependencies = new ArrayList<>();
    for (Facet facet : vajramDefinition.facetSpecs()) {
      if (facet instanceof DependencySpec definition) {
        dependencies.add(definition);
      }
    }
    Map<Dependency, KryonId> depIdToProviderKryon = new HashMap<>();
    // Create and register sub graphs for dependencies of this vajram
    for (DependencySpec dependency : dependencies) {
      var accessSpec = dependency.onVajramId();
      VajramDefinition dependencyVajram = vajramDefinitions.get(accessSpec);
      if (dependencyVajram == null) {
        throw new VajramDefinitionException(
            "Unable to find vajram for vajramId %s".formatted(accessSpec));
      }
      depIdToProviderKryon.put(
          dependency, loadKryonSubgraph(dependencyVajram.vajramId(), loadingInProgress));
    }
    return ImmutableMap.copyOf(depIdToProviderKryon);
  }

  private record InputResolverCreationResult(
      ImmutableMap<ResolverDefinition, Resolver> resolversByDefinition) {}

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
    private final Set<String> packagePrefixes = new LinkedHashSet<>();
    private final Set<Class<? extends Vajram>> classes = new LinkedHashSet<>();
    private final Map<String, OutputLogicDecoratorConfig> sessionScopedDecoratorConfigs =
        new HashMap<>();

    public VajramKryonGraphBuilder loadFromPackage(String packagePrefix) {
      packagePrefixes.add(packagePrefix);
      return this;
    }

    public VajramKryonGraphBuilder loadClass(Class<? extends Vajram> clazz) {
      classes.add(clazz);
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

    @SuppressWarnings({"UnusedMethod", "UnusedVariable", "unused"})
    // Make this private so that client use loadFromPackage instead.
    private VajramKryonGraphBuilder packagePrefixes(Set<String> packagePrefixes) {
      return this;
    }

    @SuppressWarnings({"UnusedMethod", "UnusedVariable", "unused"})
    // Make this private so that client use loadFromPackage instead.
    private VajramKryonGraphBuilder classes(Set<Class<? extends Vajram>> packagePrefixes) {
      return this;
    }

    @SuppressWarnings({"UnusedMethod", "UnusedVariable", "unused"})
    // Make this private so that client use decorateOutputLogicForSession instead.
    private VajramKryonGraphBuilder sessionScopedDecoratorConfigs(
        ImmutableMap<String, OutputLogicDecoratorConfig> sessionScopedDecoratorConfigs) {
      return this;
    }
  }
}
