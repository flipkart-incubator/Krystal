package com.flipkart.krystal.vajramexecutor.krystex;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PACKAGE;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.kryondecoration.KryonExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.batching.DepChainBatcherConfig;
import com.flipkart.krystal.vajramexecutor.krystex.batching.InputBatcherConfig;
import com.flipkart.krystal.vajramexecutor.krystex.batching.InputBatchingDecorator;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.KryonInputInjector;
import com.flipkart.krystal.vajramexecutor.krystex.traits.DefaultTraitDispatcher;
import com.google.common.collect.ImmutableList;
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

  private final @Nullable TraitDispatchDecorator traitDispatchDecorator;

  @Getter(PACKAGE)
  private final KryonExecutorConfigurator inputBatchingConfig;

  @Getter(PACKAGE)
  private final KryonExecutorConfigurator inputInjectionConfig;

  @Builder
  public KrystexGraph(
      @NonNull VajramGraph vajramGraph,
      @Nullable TraitDispatchPolicies traitDispatchPolicies,
      @Nullable InputBatcherConfig inputBatcherConfig,
      @Nullable VajramInjectionProvider injectionProvider) {
    this.vajramGraph = vajramGraph;
    this.traitDispatchPolicies =
        traitDispatchPolicies != null ? traitDispatchPolicies : new TraitDispatchPolicies();
    this.traitDispatchDecorator =
        new DefaultTraitDispatcher(vajramGraph, this.traitDispatchPolicies);
    this.inputInjectionConfig = create(injectionProvider, vajramGraph);
    this.inputBatchingConfig = create(inputBatcherConfig, vajramGraph);
  }

  public KrystexVajramExecutor createExecutor(KrystexVajramExecutorConfig vajramExecConfig) {
    vajramExecConfig.kryonExecutorConfigBuilder().traitDispatchDecorator(traitDispatchDecorator);
    return KrystexVajramExecutor.builder()
        .executableGraph(this)
        .executorConfig(vajramExecConfig)
        .build();
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
    return configBuilder ->
        configBuilder.kryonDecoratorConfig(
            KryonInputInjector.DECORATOR_TYPE,
            new KryonDecoratorConfig(
                KryonInputInjector.DECORATOR_TYPE,
                /* shouldDecorate= */ executionContext ->
                    isInjectionNeeded(executionContext, vajramGraph),
                /* instanceIdGenerator= */ executionContext -> KryonInputInjector.DECORATOR_TYPE,
                /* factory= */ decoratorContext ->
                    new KryonInputInjector(vajramGraph, injectionProvider)));
  }

  private static boolean isInjectionNeeded(
      KryonExecutionContext executionContext, VajramGraph vajramGraph) {
    return vajramGraph
        .getVajramDefinition(executionContext.vajramID())
        .metadata()
        .isInputInjectionNeeded();
  }

  private static KryonExecutorConfigurator create(
      @Nullable InputBatcherConfig inputBatcherConfig, VajramGraph vajramGraph) {
    if (inputBatcherConfig == null) {
      return KryonExecutorConfigurator.NO_OP;
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
    return configBuilder ->
        configBuilder.outputLogicDecoratorConfig(
            InputBatchingDecorator.DECORATOR_TYPE, batchingDecoratorConfig);
  }
}
