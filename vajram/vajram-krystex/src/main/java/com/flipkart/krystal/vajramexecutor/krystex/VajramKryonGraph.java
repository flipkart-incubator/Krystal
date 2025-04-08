package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.tags.ElementTags.emptyTags;
import static com.flipkart.krystal.vajram.facets.FacetValidation.validateMandatoryFacet;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.handleResolverException;
import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.toResolverCommand;
import static com.flipkart.krystal.vajram.utils.VajramLoader.loadVajramsFromClassPath;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.BasicFacetInfo;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.facets.resolution.ResolverDefinition;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.kryon.VajramKryonDefinition;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig.LogicDecoratorContext;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.krystex.resolution.Resolver;
import com.flipkart.krystal.krystex.resolution.ResolverLogic;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramDefRoot;
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
import com.flipkart.krystal.vajramexecutor.krystex.traits.TraitDispatchDecoratorImpl;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** The execution graph encompassing all registered vajrams. */
public final class VajramKryonGraph implements VajramExecutableGraph<KrystexVajramExecutorConfig> {

  @Getter private final KryonDefinitionRegistry kryonDefinitionRegistry;

  private final LogicDefRegistryDecorator logicRegistryDecorator;

  private final Map<VajramID, VajramDefinition> vajramDefinitions = new ConcurrentHashMap<>();
  private final Map<Class<? extends VajramDefRoot<?>>, VajramDefinition> definitionByDefType =
      new ConcurrentHashMap<>();
  private final Map<Class<? extends Request<?>>, VajramDefinition> definitionByReqType =
      new ConcurrentHashMap<>();

  /**
   * Maps every vajramId to its corresponding kryonId all of whose dependencies have also been
   * loaded recursively. The mapped kryon id represents the complete executable sub-graph of the
   * vajram.
   */
  private final Set<VajramID> vajramExecutables =
      Collections.synchronizedSet(new LinkedHashSet<>());

  /** LogicDecorator Id -> LogicDecoratorConfig */
  private final ImmutableMap<String, OutputLogicDecoratorConfig> sessionScopedDecoratorConfigs;

  private ImmutableMap<VajramID, TraitDispatchPolicy> traitDispatchPolicies = ImmutableMap.of();
  private @MonotonicNonNull TraitDispatchDecorator traitDispatchDecorator = null;

  @lombok.Builder
  private VajramKryonGraph(
      Set<String> packagePrefixes,
      Set<Class<? extends VajramDefRoot<?>>> classes,
      Map<String, OutputLogicDecoratorConfig> sessionScopedDecoratorConfigs) {
    this.sessionScopedDecoratorConfigs = ImmutableMap.copyOf(sessionScopedDecoratorConfigs);
    LogicDefinitionRegistry logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.kryonDefinitionRegistry = new KryonDefinitionRegistry(logicDefinitionRegistry);
    this.logicRegistryDecorator = new LogicDefRegistryDecorator(logicDefinitionRegistry);
    for (String packagePrefix : packagePrefixes) {
      loadVajramsFromClassPath(packagePrefix).forEach(this::registerVajram);
    }
    for (Class<? extends VajramDefRoot<?>> clazz : classes) {
      this.registerVajram(VajramLoader.loadVajramsFromClass(clazz));
    }
  }

  @Override
  public KrystexVajramExecutor createExecutor(KrystexVajramExecutorConfig vajramExecConfig) {
    vajramExecConfig.kryonExecutorConfigBuilder().traitDispatchDecorator(traitDispatchDecorator());
    return KrystexVajramExecutor.builder()
        .vajramKryonGraph(this)
        .executorConfig(vajramExecConfig)
        .build();
  }

  private TraitDispatchDecorator traitDispatchDecorator() {
    if (traitDispatchDecorator == null) {
      traitDispatchDecorator = new TraitDispatchDecoratorImpl(this, traitDispatchPolicies);
    }
    return traitDispatchDecorator;
  }

  public ImmutableMap<VajramID, VajramDefinition> vajramDefinitions() {
    return ImmutableMap.copyOf(vajramDefinitions);
  }

  public void registerInputBatchers(VajramID vajramID, InputBatcherConfig... inputBatcherConfigs) {
    if (vajramID == null) {
      throw new IllegalArgumentException("Unable to find vajram with id %s".formatted(vajramID));
    }
    loadKryonSubGraphIfNeeded(vajramID);
    VajramDefinition vajramDefinition = vajramDefinitions.get(vajramID);
    if (vajramDefinition == null) {
      throw new IllegalArgumentException("Unable to find vajram with id %s".formatted(vajramID));
    }
    if (vajramDefinition.isTrait()) {
      throw new VajramDefinitionException(
          "Cannot register input Batchers for vajramId %s since it is a Trait. Cannot register input batchers for traits"
              .formatted(vajramID.id()));
    }
    VajramKryonDefinition vajramKryonDefinition =
        (VajramKryonDefinition) kryonDefinitionRegistry.getOrThrow(vajramID);
    OutputLogicDefinition<Object> outputLogicDefinition =
        vajramKryonDefinition.getOutputLogicDefinition();
    VajramDefRoot<Object> vajramDef = vajramDefinition.def();
    if (!(vajramDef instanceof IOVajramDef<?>)) {
      throw new VajramDefinitionException(
          "Cannot register input Batchers for vajram %s since it is not an IOVajram"
              .formatted(vajramID.id()));
    }
    List<OutputLogicDecoratorConfig> outputLogicDecoratorConfigList = new ArrayList<>();
    for (InputBatcherConfig inputBatcherConfig : inputBatcherConfigs) {
      Predicate<LogicExecutionContext> shouldDecorate =
          logicExecutionContext -> {
            BatcherContext batcherContext =
                new BatcherContext(
                    new LogicDecoratorContext(
                        inputBatcherConfig.instanceIdGenerator().apply(logicExecutionContext),
                        logicExecutionContext));
            return inputBatcherConfig.shouldBatch().test(batcherContext)
                && vajramDefinition.metadata().isBatched();
          };
      outputLogicDecoratorConfigList.add(
          new OutputLogicDecoratorConfig(
              InputBatchingDecorator.DECORATOR_TYPE,
              shouldDecorate,
              inputBatcherConfig.instanceIdGenerator(),
              decoratorContext ->
                  inputBatcherConfig
                      .decoratorFactory()
                      .apply(new BatcherContext(decoratorContext))));
    }
    outputLogicDefinition.registerRequestScopedDecorator(outputLogicDecoratorConfigList);
  }

  public void registerTraitDispatchPolicies(TraitDispatchPolicy... traitDispatchPolicies) {
    if (!this.traitDispatchPolicies.isEmpty()) {
      throw new IllegalStateException("Trait Dispatch Policies already registered");
    }
    this.traitDispatchPolicies =
        Arrays.stream(traitDispatchPolicies)
            .collect(toImmutableMap(TraitDispatchPolicy::traitID, identity()));
  }

  public @Nullable TraitDispatchPolicy getTraitDispatchPolicy(VajramID traitID) {
    VajramDefinition traitDef = getVajramDefinition(traitID);
    if (!traitDef.isTrait()) {
      throw new IllegalArgumentException("Trait with id %s not found".formatted(traitID));
    }
    return traitDispatchPolicies.get(traitID);
  }

  /**
   * Returns a new {@link DependentChain} representing the given strings which are passed in trigger
   * order (from [Start] to immediate dependant.)
   *
   * @param firstVajramId The first vajram/kryon in the DependantChain
   * @param firstDependency The dependency of the first vajram/kryon in the DependencyChain
   * @param subsequentDependencies an array representing the dependency names in the DependantChain
   *     in trigger order
   */
  public DependentChain computeDependantChain(
      String firstVajramId, Dependency firstDependency, Dependency... subsequentDependencies) {
    VajramID firstVajramID = vajramID(firstVajramId);
    loadKryonSubGraphIfNeeded(firstVajramID);
    DependentChain currentDepChain =
        kryonDefinitionRegistry.getDependantChainsStart().extend(firstVajramID, firstDependency);
    for (Dependency dependency : subsequentDependencies) {
      currentDepChain = currentDepChain.extend(dependency.ofVajramID(), dependency);
    }
    return currentDepChain;
  }

  @Override
  public void close() {}

  /**
   * Registers vajrams that need to be executed at a later point. This is a necessary step for
   * vajram execution.
   *
   * @param vajramDef The vajram to be registered for future execution.
   * @return the {@link VajramDefinition} corresponding to the registerd vajram
   */
  private VajramDefinition registerVajram(VajramDefRoot<Object> vajramDef) {
    VajramDefinition vajramDefinition = new VajramDefinition(vajramDef);
    VajramID vajramID = vajramDefinition.vajramId();
    if (vajramDefinitions.containsKey(vajramID)) {
      return vajramDefinition;
    }
    vajramDefinitions.put(vajramID, vajramDefinition);
    definitionByDefType.putIfAbsent(vajramDefinition.defType(), vajramDefinition);
    definitionByReqType.putIfAbsent(vajramDefinition.reqRootType(), vajramDefinition);
    return vajramDefinition;
  }

  /**
   * If necessary, creates the kryons for the given vajram and, recursively for its dependencies,
   * and returns the {@link VajramID} of the {@link VajramKryonDefinition} corresponding to this
   * vajram.
   *
   * <p>This method should be called once all necessary vajrams have been registered using the
   * {@link #registerVajram(VajramDefRoot)} method. If a dependency of a vajram is not registered
   * before this step, this method will throw an exception.
   *
   * @param vajramId The id of the vajram to execute.
   */
  void loadKryonSubGraphIfNeeded(VajramID vajramId) {
    if (vajramExecutables.contains(vajramId)) {
      return;
    } else {
      loadKryonSubgraph(vajramId, new LinkedHashSet<>());
      return;
    }
  }

  private VajramID loadKryonSubgraph(VajramID vajramId, Set<VajramID> loadingInProgress) {
    synchronized (vajramExecutables) {
      if (vajramExecutables.contains(vajramId)) {
        // This means the subgraph is already loaded.
        return vajramId;
      } else if (loadingInProgress.contains(vajramId)) {
        // This means the subgraph is still being loaded, but there is a cyclic dependency. Just
        // return the vajramId to prevent infinite recursion.
        return vajramId;
      }
      // add to loadingInProgress so that this can be used to prevent infinite recursion in the
      // cases where a vajram depends on itself in a cyclic dependency.
      loadingInProgress.add(vajramId);
      VajramDefinition vajramDefinition = getVajramDefinition(vajramId);
      VajramDefRoot<Object> vajramDefRoot = vajramDefinition.def();
      ImmutableSet<? extends Facet> facets = vajramDefinition.facetSpecs();

      LogicDefinition<CreateNewRequest> createNewRequest =
          new LogicDefinition<>(
              new KryonLogicId(vajramId, "%s:newRequest"),
              ImmutableSet.of(),
              emptyTags(),
              vajramDefRoot::newRequestBuilder);
      if (vajramDefinition.isTrait()) {
        kryonDefinitionRegistry.newTraitKryonDefinition(
            vajramId.id(), facets, createNewRequest, vajramDefinition.vajramTags());
      } else if (vajramDefRoot instanceof VajramDef<Object> vajramDef) {
        InputResolverCreationResult inputResolverCreationResult =
            createKryonLogicsForInputResolvers(vajramDefinition);
        ImmutableMap<Dependency, VajramID> depIdToProviderKryon =
            createKryonDefinitionsForDependencies(vajramDefinition, loadingInProgress);
        OutputLogicDefinition<?> outputLogicDefinition =
            createKryonOutputLogic(vajramId, vajramDefinition, vajramDef);
        kryonDefinitionRegistry.newVajramKryonDefinition(
            vajramId.id(),
            facets,
            outputLogicDefinition.kryonLogicId(),
            depIdToProviderKryon,
            inputResolverCreationResult.resolversByDefinition(),
            createNewRequest,
            new LogicDefinition<>(
                new KryonLogicId(vajramId, "%s:facetsFromRequest"),
                ImmutableSet.of(),
                emptyTags(),
                r -> vajramDef.facetsFromRequest(r)),
            vajramDefinition.vajramTags());
      }
      vajramExecutables.add(vajramId);
      if (vajramDefRoot instanceof TraitDef<?>) {
        // Since this is a trait, we need to load all the conformers of this trait so that
        // invocations of this trait can be routed to the correct conforming Vajram.
        TraitDispatchPolicy traitDispatchPolicy = traitDispatchPolicies.get(vajramId);
        if (traitDispatchPolicy != null) {
          for (VajramID dispatchTarget : traitDispatchPolicy.dispatchTargets()) {
            loadKryonSubgraph(dispatchTarget, loadingInProgress);
          }
        }
      }
      return vajramId;
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
          Facet dependency = inputResolverDefinition.target().dependency();
          if (dependency == null) {
            throw new IllegalStateException();
          }
          String depName = dependency.name();

          ImmutableSet<? extends Facet> sources = inputResolver.definition().sources();
          ImmutableCollection<FacetSpec> requiredInputs =
              facetDefinitions.stream().filter(e -> sources.contains(e)).collect(toImmutableList());
          LogicDefinition<ResolverLogic> inputResolverLogic =
              logicRegistryDecorator.newResolverLogic(
                  vajramId.id(),
                  "%s:dep(%s):inputResolver(%s)"
                      .formatted(
                          vajramId,
                          dependency,
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
                                  + vajramId.id()
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
              new MandatoryFacetMissingException(vajramID.id(), mandatoryFacet.name()));
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
              value.valueOpt().orElse(null), vajramID.id(), mandatoryFacet.name());
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
      VajramID vajramID, VajramDefinition vajramDefinition, VajramDef<Object> vajramDef) {
    VajramID vajramId = vajramDefinition.vajramId();
    ImmutableSet<FacetSpec> facetSpecs = vajramDefinition.facetSpecs();
    ImmutableSet<Facet> kryonOutputLogicSources =
        facetSpecs.stream()
            .filter(vajramDefinition.outputLogicSources()::contains)
            .collect(toImmutableSet());
    KryonLogicId outputLogicName = new KryonLogicId(vajramID, "%s:outputLogic".formatted(vajramId));

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
          ImmutableMap<FacetValues, CompletableFuture<@Nullable Object>> validResults;
          try {
            validResults = vajramDef.execute(ImmutableList.copyOf(validInputs));
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
            vajramDef instanceof IOVajramDef<?>,
            outputLogicName,
            kryonOutputLogicSources,
            outputLogicCode,
            vajramDefinition.outputLogicTags());
    sessionScopedDecoratorConfigs
        .values()
        .forEach(outputLogic::registerSessionScopedLogicDecorator);
    return outputLogic;
  }

  private ImmutableMap<Dependency, VajramID> createKryonDefinitionsForDependencies(
      VajramDefinition vajramDefinition, Set<VajramID> loadingInProgress) {
    List<DependencySpec> dependencies = new ArrayList<>();
    for (Facet facet : vajramDefinition.facetSpecs()) {
      if (facet instanceof DependencySpec definition) {
        dependencies.add(definition);
      }
    }
    Map<Dependency, VajramID> depIdToProviderKryon = new HashMap<>();
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

  public VajramDefinition getVajramDefinition(VajramID vajramId) {
    VajramDefinition vajramDefinition = vajramDefinitions.get(vajramId);
    if (vajramDefinition == null) {
      throw new IllegalArgumentException(
          "Could not find vajram definition for id %s".formatted(vajramId));
    }
    return vajramDefinition;
  }

  public VajramID getVajramIdByVajramDefType(Class<? extends VajramDefRoot<?>> vajramDefClass) {
    VajramDefinition vajramDefinition = definitionByDefType.get(vajramDefClass);
    if (vajramDefinition == null) {
      throw new IllegalArgumentException(
          "Could not find vajram definition for class %s".formatted(vajramDefClass));
    }
    return vajramDefinition.vajramId();
  }

  public VajramID getVajramIdByVajramReqType(Class<? extends Request<?>> vajramDefClass) {
    VajramDefinition vajramDefinition = definitionByReqType.get(vajramDefClass);
    if (vajramDefinition == null) {
      throw new IllegalArgumentException(
          "Could not find vajram definition for request type %s".formatted(vajramDefClass));
    }
    return vajramDefinition.vajramId();
  }

  public static final class VajramKryonGraphBuilder {
    private final Set<String> packagePrefixes = new LinkedHashSet<>();
    private final Set<Class<? extends VajramDefRoot<?>>> classes = new LinkedHashSet<>();
    private final Map<String, OutputLogicDecoratorConfig> sessionScopedDecoratorConfigs =
        new HashMap<>();

    public VajramKryonGraphBuilder loadFromPackage(String packagePrefix) {
      packagePrefixes.add(packagePrefix);
      return this;
    }

    @SafeVarargs
    public final VajramKryonGraphBuilder loadClasses(Class<? extends VajramDefRoot<?>>... classes) {
      this.classes.addAll(Arrays.asList(classes));
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
    private VajramKryonGraphBuilder classes(
        Set<Class<? extends VajramDefRoot<?>>> packagePrefixes) {
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
