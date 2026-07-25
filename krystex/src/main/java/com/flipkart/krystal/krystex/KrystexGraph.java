package com.flipkart.krystal.krystex;

import static com.flipkart.krystal.krystex.batching.DepChainBatcherConfig.computeSharedBatcherConfig;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.KrystalExecutorConfig.KrystalExecutorConfigBuilder;
import com.flipkart.krystal.krystex.batching.DepChainBatcherConfig;
import com.flipkart.krystal.krystex.batching.InputBatcherConfig;
import com.flipkart.krystal.krystex.batching.InputBatcherStrategy;
import com.flipkart.krystal.krystex.batching.InputBatcherStrategy.CustomBatcherStrategy;
import com.flipkart.krystal.krystex.batching.InputBatcherStrategy.DefaultBatcherStrategy;
import com.flipkart.krystal.krystex.batching.InputBatchingDecorator;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.inputinjection.KryonInputInjector;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.krystex.kryon.TraitKryonDefinition;
import com.flipkart.krystal.krystex.kryon.VajramKryonDefinition;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.kryondecoration.KryonExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.traits.DefaultTraitDispatcher;
import com.flipkart.krystal.traits.StaticDispatchPolicy;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A directed acyclic graph which encapsulates a {@link VajramGraph} and adds the ability to create
 * executors which can execute vajrams in the graph.
 */
@Slf4j
public final class KrystexGraph {

  @Getter private final VajramGraph vajramGraph;

  private final TraitDispatchPolicies traitDispatchPolicies;

  @Getter private final @Nullable TraitDispatchDecorator traitDispatchDecorator;

  @Getter private final KryonExecutorConfigurator inputBatchingConfig;

  @Getter private final KryonExecutorConfigurator injectionConfig;

  /**
   * Maps each vajram to all the incoming dependent chains ending in that vajram which start from
   * one of {@link #externallyInvocableVajramIds()} and which are not disabled by {@link
   * #dependentChainDisabler()}
   */
  @Getter private final ImmutableMap<VajramID, Set<DependentChain>> dependentChainsByVajram;

  @Getter private final ImmutableSet<VajramID> externallyInvocableVajramIds;

  @Getter private final DependentChainDisabler dependentChainDisabler;

  /**
   * @param vajramGraph
   * @param traitDispatchPolicies
   * @param inputBatcherStrategy
   * @param injectionProvider
   * @param dependentChainDisabler used to determine which {@link DependentChain}s are disabled
   */
  @Builder
  private KrystexGraph(
      @NonNull VajramGraph vajramGraph,
      @Nullable TraitDispatchPolicies traitDispatchPolicies,
      @Nullable InputBatcherStrategy inputBatcherStrategy,
      @Nullable VajramInjectionProvider injectionProvider,
      @Nullable ImmutableSet<VajramID> externallyInvocableVajramIds,
      @Nullable DependentChainDisabler dependentChainDisabler) {
    this.vajramGraph = vajramGraph;
    this.traitDispatchPolicies =
        requireNonNullElseGet(traitDispatchPolicies, TraitDispatchPolicies::new);
    this.traitDispatchDecorator =
        new DefaultTraitDispatcher(this.vajramGraph, this.traitDispatchPolicies);
    this.dependentChainDisabler =
        requireNonNullElse(dependentChainDisabler, DependentChainDisabler.DISABLE_NONE);
    this.injectionConfig = create(injectionProvider, this.vajramGraph);
    this.externallyInvocableVajramIds =
        requireNonNullElseGet(
            externallyInvocableVajramIds,
            () ->
                vajramGraph.vajramDefinitions().values().stream()
                    .filter(
                        v ->
                            v.vajramTags()
                                .getAnnotationByType(InvocableOutsideGraph.class)
                                .isPresent())
                    .map(VajramDefinition::vajramId)
                    .collect(toImmutableSet()));
    this.inputBatchingConfig =
        create(
            inputBatcherStrategy,
            this.vajramGraph,
            this.externallyInvocableVajramIds,
            this.traitDispatchPolicies,
            this.dependentChainDisabler);
    this.dependentChainsByVajram =
        computeIncomingDependentChains(
            this.vajramGraph,
            this.traitDispatchDecorator,
            this.dependentChainDisabler,
            this.externallyInvocableVajramIds);
  }

  public VajramKryonExecutor createExecutor(KrystalExecutorConfigBuilder vajramExecConfig) {
    KrystalExecutorConfigBuilder executorConfigBuilder =
        vajramExecConfig.configureWith(inputBatchingConfig).configureWith(injectionConfig);
    if (traitDispatchDecorator != null) {
      vajramExecConfig.traitDispatchDecorator(traitDispatchDecorator);
    }
    return new VajramKryonExecutor(this, executorConfigBuilder);
  }

  public @Nullable TraitDispatchPolicy getTraitDispatchPolicy(VajramID traitID) {
    VajramDefinition traitDef = vajramGraph.getVajramDefinition(traitID);
    if (!traitDef.isTrait()) {
      throw new IllegalArgumentException("Vajram with id %s is not a trait!".formatted(traitID));
    }
    return traitDispatchPolicies.get(traitID);
  }

  private static KryonExecutorConfigurator create(
      @Nullable VajramInjectionProvider injectionProvider, VajramGraph vajramGraph) {
    if (injectionProvider == null) {
      return KryonExecutorConfigurator.NO_OP;
    }
    return configBuilder -> {
      String decoratorType = KryonInputInjector.DECORATOR_TYPE;
      if (configBuilder.hasKryonDecorator(decoratorType)) {
        // The decorator set in the executor config has higher precedence
        // than the one set in the Graph
        return;
      }
      configBuilder.kryonDecoratorConfig(
          new KryonDecoratorConfig(
              decoratorType,
              /* shouldDecorate= */ executionContext ->
                  isInjectionNeeded(executionContext, vajramGraph),
              /* instanceIdGenerator= */ executionContext -> decoratorType,
              /* factory= */ decoratorContext ->
                  new KryonInputInjector(vajramGraph, injectionProvider)));
    };
  }

  private static boolean isInjectionNeeded(
      KryonExecutionContext executionContext, VajramGraph vajramGraph) {
    return vajramGraph
        .getVajramDefinition(executionContext.vajramID())
        .metadata()
        .isInputInjectionNeeded();
  }

  private static KryonExecutorConfigurator create(
      @Nullable InputBatcherStrategy inputBatcherStrategy,
      VajramGraph vajramGraph,
      ImmutableSet<VajramID> externallyInvocableVajramIds,
      TraitDispatchPolicies traitDispatchPolicies,
      DependentChainDisabler dependentChainDisabler) {
    InputBatcherConfig inputBatcherConfig;
    if (inputBatcherStrategy == null) {
      return KryonExecutorConfigurator.NO_OP;
    } else if (inputBatcherStrategy instanceof CustomBatcherStrategy customStrategy) {
      inputBatcherConfig = customStrategy.customBatcherConfig();
    } else if (inputBatcherStrategy instanceof DefaultBatcherStrategy defaultStrategy) {
      inputBatcherConfig =
          computeSharedBatcherConfig(
              vajramGraph,
              defaultStrategy.batchSizeSupplier(),
              traitDispatchPolicies,
              dependentChainDisabler,
              externallyInvocableVajramIds);
    } else {
      throw new AssertionError("Not possible");
    }
    ConcurrentHashMap<DependentChain, DepChainBatcherConfig> batcherConfigByDepChain =
        new ConcurrentHashMap<>();

    Function<LogicExecutionContext, DepChainBatcherConfig> inputBatcherForLogicExecContext =
        logicExecutionContext ->
            batcherConfigByDepChain.computeIfAbsent(
                logicExecutionContext.dependents(),
                d -> {
                  VajramID vajramID = logicExecutionContext.vajramID();
                  VajramDefinition vajramDefinition = vajramGraph.vajramDefinitions().get(vajramID);
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

    String decoratorType = InputBatchingDecorator.DECORATOR_TYPE;
    OutputLogicDecoratorConfig batchingDecoratorConfig =
        new OutputLogicDecoratorConfig(
            decoratorType,
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
    return configBuilder -> {
      if (configBuilder.hasOutputLogicDecorator(decoratorType)) {
        // The decorator set in the executor config has higher precedence
        // than the one set in the Graph
        return;
      }
      configBuilder.outputLogicDecoratorConfig(batchingDecoratorConfig);
    };
  }

  private static ImmutableMap<VajramID, Set<DependentChain>> computeIncomingDependentChains(
      VajramGraph vajramGraph,
      TraitDispatchDecorator traitDispatchDecorator,
      DependentChainDisabler dependentChainDisabler,
      ImmutableSet<VajramID> externallyInvocableVajramIds) {
    Map<VajramID, Set<DependentChain>> dependentChainsByVajramId = new HashMap<>();
    DependentChain depChain = vajramGraph.kryonDefinitionRegistry().getDependentChainsStart();
    for (VajramID vajramID : externallyInvocableVajramIds) {
      _computeIncomingDependentChains(
          vajramID,
          dependentChainsByVajramId,
          depChain,
          vajramGraph,
          traitDispatchDecorator,
          dependentChainDisabler);
    }
    return ImmutableMap.copyOf(dependentChainsByVajramId);
  }

  private static void _computeIncomingDependentChains(
      VajramID vajramID,
      Map<VajramID, Set<DependentChain>> dependentChainsByVajramId,
      DependentChain incomingDependentChain,
      VajramGraph vajramGraph,
      TraitDispatchDecorator traitDispatchDecorator,
      DependentChainDisabler dependentChainDisabler) {
    if (dependentChainDisabler.isDisabled(incomingDependentChain)) {
      // If a dependantChain is disabled, don't create further depChains
      return;
    }
    KryonDefinitionRegistry kryonDefinitionRegistry = vajramGraph.kryonDefinitionRegistry();
    List<VajramID> concreteVajramIds = new ArrayList<>();
    if (kryonDefinitionRegistry.get(vajramID) instanceof TraitKryonDefinition) {
      @Nullable Dependency dependency = incomingDependentChain.latestDependency();
      @Nullable TraitDispatchPolicy traitDispatchPolicy =
          traitDispatchDecorator.traitDispatchPolicies().get(vajramID);
      if (traitDispatchPolicy != null) {
        if (traitDispatchPolicy instanceof StaticDispatchPolicy staticDispatchPolicy
            && dependency != null) {
          concreteVajramIds.add(staticDispatchPolicy.getDispatchTargetID(dependency));
        } else {
          concreteVajramIds.addAll(traitDispatchPolicy.dispatchTargetIDs());
          dependentChainsByVajramId
              .computeIfAbsent(vajramID, _n -> new LinkedHashSet<>())
              .add(incomingDependentChain);
        }
      }
    } else {
      concreteVajramIds.add(vajramID);
    }
    for (VajramID finalVajramId : concreteVajramIds) {
      ImmutableSet<Dependency> dependencies = ImmutableSet.of();
      if (kryonDefinitionRegistry.get(finalVajramId)
          instanceof VajramKryonDefinition vajramKryonDefinition) {
        dependencies = vajramKryonDefinition.dependencies();
      }
      for (Dependency dependency : dependencies) {
        _computeIncomingDependentChains(
            dependency.onVajramID(),
            dependentChainsByVajramId,
            incomingDependentChain.extend(finalVajramId, dependency),
            vajramGraph,
            traitDispatchDecorator,
            dependentChainDisabler);
      }
      dependentChainsByVajramId
          .computeIfAbsent(finalVajramId, _n -> new LinkedHashSet<>())
          .add(incomingDependentChain);
    }
  }

  public static class KrystexGraphBuilder {

    private TraitDispatchPolicies traitDispatchPolicies = new TraitDispatchPolicies();

    public KrystexGraphBuilder traitDispatchPolicies(TraitDispatchPolicy... traitDispatchPolicies) {
      this.traitDispatchPolicies = this.traitDispatchPolicies.merge(traitDispatchPolicies);
      return this;
    }

    public KrystexGraphBuilder traitDispatchPolicies(TraitDispatchPolicies traitDispatchPolicies) {
      this.traitDispatchPolicies = this.traitDispatchPolicies.merge(traitDispatchPolicies);
      return this;
    }

    public KrystexGraphBuilder traitDispatchPolicies(
        Collection<? extends TraitDispatchPolicy> traitDispatchPolicies) {
      this.traitDispatchPolicies = this.traitDispatchPolicies.merge(traitDispatchPolicies);
      return this;
    }

    public TraitDispatchPolicies traitDispatchPolicies() {
      return this.traitDispatchPolicies;
    }
  }
}
