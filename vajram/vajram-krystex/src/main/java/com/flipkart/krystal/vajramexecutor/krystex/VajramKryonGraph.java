package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.facets.resolution.ResolverCommand.skip;
import static com.flipkart.krystal.tags.ElementTags.emptyTags;
import static com.flipkart.krystal.vajram.facets.FacetValidation.validateMandatoryFacet;
import static com.flipkart.krystal.vajram.utils.VajramLoader.loadVajramsFromClassPath;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.core.OutputLogicExecutionResults;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.ImmutableFacetValues;
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
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.kryon.VajramKryonDefinition;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.kryondecoration.KryonExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.krystex.resolution.Resolver;
import com.flipkart.krystal.krystex.resolution.ResolverLogic;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajram.IOVajramDef;
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
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.flipkart.krystal.vajram.utils.VajramLoader;
import com.flipkart.krystal.vajramexecutor.krystex.batching.DepChainBatcherConfig;
import com.flipkart.krystal.vajramexecutor.krystex.batching.InputBatcherConfig;
import com.flipkart.krystal.vajramexecutor.krystex.batching.InputBatchingDecorator;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.KryonInputInjector;
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
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** The execution graph encompassing all registered vajrams. */
@Slf4j
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

  private ImmutableMap<VajramID, TraitDispatchPolicy> traitDispatchPolicies = ImmutableMap.of();
  private @MonotonicNonNull TraitDispatchDecorator traitDispatchDecorator = null;

  @Getter private KryonExecutorConfigurator inputBatchingConfig = KryonExecutorConfigurator.NO_OP;
  @Getter private KryonExecutorConfigurator inputInjectionConfig = KryonExecutorConfigurator.NO_OP;

  @lombok.Builder
  private VajramKryonGraph(
      Set<String> packagePrefixes, Set<Class<? extends VajramDefRoot<?>>> classes) {
    LogicDefinitionRegistry logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.kryonDefinitionRegistry = new KryonDefinitionRegistry(logicDefinitionRegistry);
    this.logicRegistryDecorator = new LogicDefRegistryDecorator(logicDefinitionRegistry);
    for (String packagePrefix : packagePrefixes) {
      loadVajramsFromClassPath(packagePrefix).forEach(this::registerVajram);
    }
    for (Class<? extends VajramDefRoot<?>> clazz : classes) {
      this.registerVajram(VajramLoader.createVajramObjectForClass(clazz));
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

  public void registerInputInjector(VajramInjectionProvider injectionProvider) {
    if (this.inputInjectionConfig != KryonExecutorConfigurator.NO_OP) {
      throw new IllegalArgumentException(
          "Re-registering input injector config in not currently allowed.");
    }
    if (injectionProvider == null) {
      return;
    }
    this.inputInjectionConfig =
        configBuilder ->
            configBuilder.kryonDecoratorConfig(
                KryonInputInjector.DECORATOR_TYPE,
                new KryonDecoratorConfig(
                    KryonInputInjector.DECORATOR_TYPE,
                    /* shouldDecorate= */ this::isInjectionNeeded,
                    /* instanceIdGenerator= */ executionContext ->
                        KryonInputInjector.DECORATOR_TYPE,
                    /* factory= */ decoratorContext ->
                        new KryonInputInjector(this, injectionProvider)));
  }

  private boolean isInjectionNeeded(KryonExecutionContext executionContext) {
    return getVajramDefinition(executionContext.vajramID()).metadata().isInputInjectionNeeded();
  }

  public void registerInputBatchers(InputBatcherConfig inputBatcherConfig) {
    if (this.inputBatchingConfig != KryonExecutorConfigurator.NO_OP) {
      throw new IllegalArgumentException(
          "Re-registering input batcher config in not currently allowed.");
    }
    ConcurrentHashMap<DependentChain, DepChainBatcherConfig> batcherConfigByDepChain =
        new ConcurrentHashMap<>();

    Function<LogicExecutionContext, DepChainBatcherConfig> inputBatcherForLogicExecContext =
        logicExecutionContext ->
            batcherConfigByDepChain.computeIfAbsent(
                logicExecutionContext.dependents(),
                d -> {
                  VajramID vajramID = logicExecutionContext.vajramID();
                  VajramDefinition vajramDefinition = vajramDefinitions.get(vajramID);
                  if (vajramDefinition == null) {
                    log.error(
                        "Unable to find vajram with id {}. Something is wrong. Skipping InputBatchingDecorator application.",
                        vajramID);
                    return DepChainBatcherConfig.NO_BATCHING;
                  }
                  if (vajramDefinition.isTrait()) {
                    log.error(
                        "Cannot register input Batchers for vajramId {} since it is a Trait. Skipping InputBatchingDecorator application.",
                        vajramID.id());
                    return DepChainBatcherConfig.NO_BATCHING;
                  }
                  for (DepChainBatcherConfig depChainBatcherConfig :
                      inputBatcherConfig
                          .depChainBatcherConfigs()
                          .getOrDefault(vajramID, ImmutableList.of())) {
                    boolean shouldDecorate =
                        vajramDefinition.metadata().isBatched()
                            && depChainBatcherConfig.shouldBatch().test(logicExecutionContext);
                    if (shouldDecorate) {
                      return depChainBatcherConfig;
                    }
                  }
                  return DepChainBatcherConfig.NO_BATCHING;
                });

    OutputLogicDecoratorConfig batchingDecoratorConfig =
        new OutputLogicDecoratorConfig(
            InputBatchingDecorator.DECORATOR_TYPE,
            logicExecutionContext ->
                !DepChainBatcherConfig.NO_BATCHING.equals(
                    inputBatcherForLogicExecContext.apply(logicExecutionContext)),
            logicExecutionContext ->
                requireNonNull(inputBatcherForLogicExecContext.apply(logicExecutionContext))
                    .instanceIdGenerator()
                    .apply(logicExecutionContext),
            decoratorContext ->
                requireNonNull(
                        inputBatcherForLogicExecContext.apply(
                            decoratorContext.logicExecutionContext()))
                    .decoratorFactory()
                    .apply(decoratorContext));
    this.inputBatchingConfig =
        configBuilder ->
            configBuilder.outputLogicDecoratorConfig(
                InputBatchingDecorator.DECORATOR_TYPE, batchingDecoratorConfig);
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
  public DependentChain computeDependentChain(
      String firstVajramId, Dependency firstDependency, Dependency... subsequentDependencies) {
    DependentChain currentDepChain =
        kryonDefinitionRegistry
            .getDependentChainsStart()
            .extend(vajramID(firstVajramId), firstDependency);
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
   * @return the {@link VajramDefinition} corresponding to the registered vajram
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

        createKryonDefinitionsForDependencies(vajramDefinition, loadingInProgress);
        OutputLogicDefinition<?> outputLogicDefinition =
            createKryonOutputLogic(vajramId, vajramDefinition, vajramDef);
        kryonDefinitionRegistry.newVajramKryonDefinition(
            vajramId,
            facets,
            outputLogicDefinition.kryonLogicId(),
            inputResolverCreationResult.resolversByDefinition(),
            createNewRequest,
            new LogicDefinition<>(
                new KryonLogicId(vajramId, "%s:facetsFromRequest"),
                ImmutableSet.of(),
                emptyTags(),
                vajramDef::facetsFromRequest),
            vajramDef::executeGraph,
            vajramDefinition.vajramTags());
      }
      vajramExecutables.add(vajramId);
      if (vajramDefinition.isTrait()) {
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
              facetDefinitions.stream().filter(sources::contains).collect(toImmutableList());
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
                          skip(
                              "Got exception '%s' while executing the resolver of the dependency '%s'"
                                  .formatted(t, depName),
                              t);
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
        requiredInputs.stream()
                .filter(
                    facetSpec ->
                        facetSpec
                            .tags()
                            .getAnnotationByType(IfAbsent.class)
                            .map(ifAbsent -> IfAbsentThen.FAIL.equals(ifAbsent.value()))
                            .orElse(false))
            ::iterator;
    Map<String, Throwable> missingMandatoryValues = new HashMap<>();
    for (Facet mandatoryFacet : mandatoryFacets) {
      FacetValue facetValue = mandatoryFacet.getFacetValue(facetValues);
      Errable<?> value;
      if (facetValue instanceof Errable<?> errable) {
        value = errable;
      } else if (facetValue instanceof One2OneDepResponse<?, ?> depResponse) {
        value = depResponse.response();
      } else if (facetValue instanceof FanoutDepResponses<?, ?> fanoutDepResponses) {
        var requestResponsePairs = fanoutDepResponses.requestResponsePairs();
        if (requestResponsePairs.stream()
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
        input -> {
          ImmutableList<? extends FacetValues> inputsList = input.facetValues();
          List<FacetValues> validInputs = new ArrayList<>();
          Map<ImmutableFacetValues, CompletableFuture<@Nullable Object>> failedValidations =
              new LinkedHashMap<>();
          inputsList.forEach(
              inputs -> {
                try {
                  validateMandatory(vajramId, inputs, facetSpecs);
                  validInputs.add(inputs);
                } catch (Throwable e) {
                  failedValidations.put(inputs._build(), failedFuture(e));
                }
              });
          OutputLogicExecutionResults<Object> validResults;
          try {
            validResults = vajramDef.execute(input.withFacetValues(validInputs));
          } catch (Throwable e) {
            return new OutputLogicExecutionResults<>(
                validInputs.stream()
                    .collect(toImmutableMap(FacetValues::_build, i -> failedFuture(e))));
          }

          return validResults.withResults(
              ImmutableMap.<ImmutableFacetValues, CompletableFuture<@Nullable Object>>builder()
                  .putAll(validResults.results())
                  .putAll(failedValidations)
                  .build());
        };
    return logicRegistryDecorator.newOutputLogic(
        vajramDef instanceof IOVajramDef<?>,
        outputLogicName,
        kryonOutputLogicSources,
        outputLogicCode,
        vajramDefinition.outputLogicTags());
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
      var accessSpec = dependency.onVajramID();
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

  public VajramID getVajramIdByVajramReqType(Class<? extends Request<?>> vajramReqClass) {
    VajramDefinition vajramDefinition = definitionByReqType.get(vajramReqClass);
    if (vajramDefinition == null) {
      throw new IllegalArgumentException(
          "Could not find vajram definition for request type %s".formatted(vajramReqClass));
    }
    return vajramDefinition.vajramId();
  }

  public Optional<Class<? extends Request<?>>> getVajramReqByVajramId(VajramID vajramID) {
    return Optional.ofNullable(vajramDefinitions.get(vajramID)).map(VajramDefinition::reqRootType);
  }

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  public static final class VajramKryonGraphBuilder {
    private final Set<String> packagePrefixes = new LinkedHashSet<>();
    private final Set<Class<? extends VajramDefRoot<?>>> classes = new LinkedHashSet<>();

    public VajramKryonGraphBuilder loadFromPackage(String packagePrefix) {
      packagePrefixes.add(packagePrefix);
      return this;
    }

    @SafeVarargs
    public final VajramKryonGraphBuilder loadClasses(Class<? extends VajramDefRoot<?>>... classes) {
      this.classes.addAll(Arrays.asList(classes));
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
  }
}
