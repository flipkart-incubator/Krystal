package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.tags.ElementTags.emptyTags;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.handleResolverException;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.toResolverCommand;
import static com.flipkart.krystal.vajram.utils.VajramLoader.loadVajramsFromClassPath;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.data.Success;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
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
import com.flipkart.krystal.krystex.resolution.DependencyResolutionRequest;
import com.flipkart.krystal.krystex.resolution.MultiResolver;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverLogic;
import com.flipkart.krystal.resolution.ResolverCommand;
import com.flipkart.krystal.vajram.BatchableVajram;
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
import com.flipkart.krystal.vajram.facets.DependencyDef;
import com.flipkart.krystal.vajram.facets.InputDef;
import com.flipkart.krystal.vajram.facets.VajramFacetDefinition;
import com.flipkart.krystal.vajram.facets.resolution.FanoutInputResolver;
import com.flipkart.krystal.vajram.facets.resolution.InputResolverDefinition;
import com.flipkart.krystal.vajram.facets.resolution.SingleInputResolver;
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
      Set<String> packagePrefixes,
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
                && vajram.getFacetDefinitions().stream()
                    .filter(facetDefinition -> facetDefinition instanceof InputDef<?>)
                    .map(facetDefinition -> (InputDef<?>) facetDefinition)
                    .anyMatch(InputDef::isBatched);
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
      String firstVajramId, int firstDependencyId, int... subsequentDependencyIds) {
    KryonId firstKryonId = _getVajramExecutionGraph(vajramID(firstVajramId));
    KryonDefinition currentKryon = kryonDefinitionRegistry.get(firstKryonId);
    DependantChain currentDepChain =
        kryonDefinitionRegistry.getDependantChainsStart().extend(firstKryonId, firstDependencyId);
    int previousDepName = firstDependencyId;
    for (int currentDepName : subsequentDependencyIds) {
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

    ImmutableMap<Integer, KryonId> depIdToProviderKryon =
        createKryonDefinitionsForDependencies(vajramDefinition);

    OutputLogicDefinition<?> outputLogicDefinition =
        createKryonOutputLogic(kryonId, vajramDefinition);

    ImmutableSet<Integer> inputIds =
        vajramDefinition.vajram().getFacetDefinitions().stream()
            .filter(vajramFacetDefinition -> vajramFacetDefinition instanceof InputDef<?>)
            .map(VajramFacetDefinition::id)
            .collect(toImmutableSet());

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
            inputResolverCreationResult.resolverDefinitionsById(),
            createNewRequest,
            new LogicDefinition<>(
                new KryonLogicId(kryonId, "%s:facetsFromRequest"),
                ImmutableSet.of(),
                emptyTags(),
                r -> vajramDefinition.vajram().facetsFromRequest(r)),
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
    ImmutableMap<Integer, InputResolverDefinition> inputResolvers =
        vajramDefinition.inputResolverDefinitions();

    Map<Integer, ResolverDefinition> resolverDefinitions = new LinkedHashMap<>();
    inputResolvers.forEach(
        (resolverId, inputResolverDefinition) -> {
          int dependencyId = inputResolverDefinition.resolutionTarget().dependencyId();
          VajramFacetDefinition vajramFacetDefinition =
              vajramDefinition.facetsById().get(dependencyId);
          if (vajramFacetDefinition == null) {
            throw new IllegalStateException();
          }
          String depName = vajramFacetDefinition.name();
          ImmutableSet<String> resolvedInputNames =
              inputResolverDefinition.resolutionTarget().inputNames();
          ImmutableSet<Integer> resolvedInputIds =
              resolvedInputNames.stream()
                  .map(s -> Optional.ofNullable(vajramDefinition.facetsByName().get(s)))
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .map(VajramFacetDefinition::id)
                  .collect(toImmutableSet());

          ImmutableSet<Integer> sources = inputResolverDefinition.sources();
          ImmutableCollection<VajramFacetDefinition> requiredInputs =
              facetDefinitions.stream()
                  .filter(def -> sources.contains(def.id()))
                  .collect(toImmutableList());
          LogicDefinition<ResolverLogic> inputResolverLogic =
              logicRegistryDecorator.newResolverLogic(
                  vajramId.vajramId(),
                  "%s:dep(%s):inputResolver(%s)"
                      .formatted(
                          vajramId,
                          dependencyId,
                          String.join(
                              ",", resolvedInputNames.stream().map(String::valueOf).toList())),
                  sources,
                  (depRequests, facets) -> {
                    validateMandatory(vajramId, facets, requiredInputs);
                    ResolverCommand resolverCommand = ResolverCommand.skip("");
                    try {
                      if (inputResolverDefinition
                          instanceof SingleInputResolver singleInputResolver) {
                        resolverCommand = singleInputResolver.resolve(depRequests, facets);
                      } else if (inputResolverDefinition
                          instanceof FanoutInputResolver fanoutInputResolver) {
                        if (depRequests.size() != 1) {
                          throw new IllegalStateException(
                              "The vajram "
                                  + vajramId.vajramId()
                                  + " can have at most one fanout resolver");
                        }
                        resolverCommand =
                            fanoutInputResolver.resolve(depRequests.get(0)._build(), facets);
                      }
                    } catch (Throwable t) {
                      resolverCommand =
                          toResolverCommand(
                              handleResolverException(
                                  t,
                                  false,
                                  "Got exception while executing the resolver of the dependency "
                                      + depName));
                    }
                    return resolverCommand;
                  });
          ResolverDefinition resolverDefinition =
              new ResolverDefinition(
                  resolverId,
                  inputResolverLogic.kryonLogicId(),
                  sources,
                  dependencyId,
                  resolvedInputIds);
          resolverDefinitions.put(resolverDefinition.resolverId(), resolverDefinition);
        });
    LogicDefinition<MultiResolver> multiResolverDefinition =
        logicRegistryDecorator.newMultiResolver(
            vajramId.vajramId(),
            vajramId.vajramId() + ":multiResolver",
            facetDefinitions.stream().map(VajramFacetDefinition::id).collect(toImmutableSet()),
            (resolutionRequests, facets) -> {
              Map<Integer, List<InputResolverDefinition>> resolversByDep = new HashMap<>();
              for (DependencyResolutionRequest resolutionRequest : resolutionRequests) {
                for (int resolverId : resolutionRequest.resolverIds()) {
                  InputResolverDefinition resolverDefinition = inputResolvers.get(resolverId);
                  if (resolverDefinition != null) {
                    resolversByDep
                        .computeIfAbsent(resolutionRequest.dependencyId(), _k -> new ArrayList<>())
                        .add(resolverDefinition);
                  }
                }
              }
              Map<Integer, ResolverCommand> resolverCommandsByDep = new LinkedHashMap<>();
              resolversByDep.forEach(
                  (depId, resolvers) -> {
                    ImmutableList<RequestBuilder<Object>> depRequests =
                        ImmutableList.of(newRequestForDependency(vajramDefinition, depId));
                    for (InputResolverDefinition inputResolverDefinition : resolvers) {
                      ResolverCommand resolverCommand = ResolverCommand.skip("");
                      try {
                        if (inputResolverDefinition
                            instanceof SingleInputResolver singleInputResolver) {
                          resolverCommand = singleInputResolver.resolve(depRequests, facets);
                        } else if (inputResolverDefinition
                            instanceof FanoutInputResolver fanoutInputResolver) {
                          if (depRequests.size() != 1) {
                            throw new IllegalStateException(
                                "The vajram "
                                    + vajramId.vajramId()
                                    + " can have at most one fanout resolver");
                          }
                          resolverCommand =
                              fanoutInputResolver.resolve(depRequests.get(0)._build(), facets);
                        }
                      } catch (Throwable t) {
                        resolverCommand = toResolverCommand(handleResolverException(t, false));
                      }
                      resolverCommandsByDep.put(depId, resolverCommand);
                    }
                  });
              return ImmutableMap.copyOf(resolverCommandsByDep);
            });
    return new InputResolverCreationResult(
        ImmutableMap.copyOf(resolverDefinitions), multiResolverDefinition.kryonLogicId());
  }

  private RequestBuilder<Object> newRequestForDependency(
      VajramDefinition vajramDefinition, int dependencyId) {
    return checkNotNull(
            vajramDefinitions.get(
                (VajramID)
                    ((DependencyDef<?>)
                            checkNotNull(vajramDefinition.facetsById().get(dependencyId)))
                        .dataAccessSpec()))
        .vajram()
        .newRequestBuilder();
  }

  private void validateMandatory(
      VajramID vajramID, Facets facets, ImmutableCollection<VajramFacetDefinition> requiredInputs) {
    @SuppressWarnings("StreamToIterable")
    Iterable<VajramFacetDefinition> mandatoryFacets =
        requiredInputs.stream().filter(VajramFacetDefinition::isMandatory)::iterator;
    Map<String, Throwable> missingMandatoryValues = new HashMap<>();
    for (VajramFacetDefinition mandatoryFacet : mandatoryFacets) {
      FacetValue<?> value = facets._get(mandatoryFacet.id());
      if (value instanceof Errable<?> e) {
        if (!(e instanceof Success<?>)) {
          missingMandatoryValues.put(
              mandatoryFacet.name(),
              e.errorOpt()
                  .orElse(
                      new NoSuchElementException(
                          "No value present for input '%s'".formatted(mandatoryFacet.name()))));
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
    ImmutableCollection<VajramFacetDefinition> facetDefinitions =
        vajramDefinition.vajram().getFacetDefinitions();
    ImmutableSet<Integer> kryonOutputLogicSources =
        facetDefinitions.stream()
            .map(VajramFacetDefinition::id)
            .filter(vajramDefinition.outputLogicSources()::contains)
            .collect(toImmutableSet());
    KryonLogicId outputLogicName = new KryonLogicId(kryonId, "%s:outputLogic".formatted(vajramId));

    // Step 4: Create and register Kryon for the output logic
    OutputLogicDefinition<?> outputLogic =
        logicRegistryDecorator.newOutputLogic(
            vajramDefinition.vajram() instanceof IOVajram<?>,
            outputLogicName,
            kryonOutputLogicSources,
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
              Vajram<Object> vajram = vajramDefinition.vajram();
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

  private ImmutableMap<Integer, KryonId> createKryonDefinitionsForDependencies(
      VajramDefinition vajramDefinition) {
    List<DependencyDef<?>> dependencies = new ArrayList<>();
    for (VajramFacetDefinition vajramFacetDefinition :
        vajramDefinition.vajram().getFacetDefinitions()) {
      if (vajramFacetDefinition instanceof DependencyDef<?> definition) {
        dependencies.add(definition);
      }
    }
    Map<Integer, KryonId> depIdToProviderKryon = new HashMap<>();
    // Create and register sub graphs for dependencies of this vajram
    for (DependencyDef<?> dependencyDef : dependencies) {
      var accessSpec = dependencyDef.dataAccessSpec();
      int dependencyName = dependencyDef.id();
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

      depIdToProviderKryon.put(
          dependencyName, _getVajramExecutionGraph(dependencyVajram.vajramId()));
    }
    return ImmutableMap.copyOf(depIdToProviderKryon);
  }

  private record InputResolverCreationResult(
      ImmutableMap<Integer, ResolverDefinition> resolverDefinitionsById,
      KryonLogicId multiResolver) {}

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

    @SuppressWarnings({"UnusedMethod", "UnusedVariable", "unused"})
    // Make this private so that client use loadFromPackage instead.
    private VajramKryonGraphBuilder packagePrefixes(Set<String> packagePrefixes) {
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
