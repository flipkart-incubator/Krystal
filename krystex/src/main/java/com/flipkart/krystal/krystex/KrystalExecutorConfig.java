package com.flipkart.krystal.krystex;

import static com.flipkart.krystal.krystex.kryon.VajramKryonExecutor.GraphTraversalStrategy.BREADTH;
import static com.flipkart.krystal.krystex.kryon.VajramKryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.VajramKryonExecutor.KryonExecStrategy.DIRECT;

import com.flipkart.krystal.annos.TraitDependency;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecoratorConfig;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.krystex.kryon.KrystalExecutorExecutionInfo;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;

/**
 * This is used to configure a particular execution of the Krystal graph.
 *
 * @param decorationOrdering The ordering of logic decorators to be applied
 * @param outputLogicDecoratorConfigs The request scoped logic decorators to be applied
 * @param disabledDependentChains Invocation originating for these dependant chains will be blocked.
 *     Useful for avoid graph size explosion in case of recursive dependencies
 * @param kryonExecStrategy Currently on BATCH is supported. More might be added in future
 * @param graphTraversalStrategy DEPTH is more performant and memory efficient. BREADTH is sometimes
 *     useful for debugging
 * @param kryonDecoratorConfigs The request scoped kryon decorators to be applied
 * @param executorService This is used as the event loop for the message passing within this
 *     execution.
 * @param executorServiceTransformer Used to transform the {@link #executorService} so that it can
 *     be wrapped with additional functionality like context propagation. The transformed {@link
 *     ExecutorService}(s) MUST eventually delegate all tasks to the same {@link
 *     SingleThreadExecutor} instance which is provided to the {@link #executorService}. Else this
 *     can lead unpredictable behaviour.
 * @param traitDispatchDecorator used to determine the conformant vajrams bound to traits
 * @param debug If true, more human-readable names are give to entities and logging is more verbose
 *     - might have a performance impact.
 */
public record KrystalExecutorConfig(
    @Nullable String executorId,
    DecorationOrdering decorationOrdering,
    ImmutableSet<DependentChain> disabledDependentChains,
    KryonExecStrategy kryonExecStrategy,
    GraphTraversalStrategy graphTraversalStrategy,
    Map<String, OutputLogicDecoratorConfig> outputLogicDecoratorConfigs,
    Map<String, KryonDecoratorConfig> kryonDecoratorConfigs,
    Map<String, DependencyDecoratorConfig> dependencyDecoratorConfigs,
    KrystalExecutorExecutionInfo executorInfo,
    @NonNull SingleThreadExecutor executorService,
    Function<ExecutorService, ExecutorService> executorServiceTransformer,
    @Nullable TraitDispatchDecorator traitDispatchDecorator,
    boolean debug) {

  @Builder
  public KrystalExecutorConfig {
    if (kryonExecStrategy == null) {
      kryonExecStrategy = DIRECT;
    }
    if (graphTraversalStrategy == null) {
      graphTraversalStrategy = DEPTH;
    }
    if (kryonExecStrategy == DIRECT && graphTraversalStrategy == BREADTH) {
      throw new UnsupportedOperationException(
          "DIRECT kryon execution only supports DEPTH traversal strategy.");
    }
    if (kryonDecoratorConfigs == null) {
      kryonDecoratorConfigs = ImmutableMap.of();
    } else {
      kryonDecoratorConfigs = ImmutableMap.copyOf(kryonDecoratorConfigs);
    }
    if (dependencyDecoratorConfigs == null) {
      dependencyDecoratorConfigs = ImmutableMap.of();
    } else {
      dependencyDecoratorConfigs = ImmutableMap.copyOf(dependencyDecoratorConfigs);
    }
    if (outputLogicDecoratorConfigs == null) {
      outputLogicDecoratorConfigs = ImmutableMap.of();
    } else {
      outputLogicDecoratorConfigs = ImmutableMap.copyOf(outputLogicDecoratorConfigs);
    }
    if (disabledDependentChains == null) {
      disabledDependentChains = ImmutableSet.of();
    }
    if (decorationOrdering == null) {
      decorationOrdering = DecorationOrdering.none();
    }
    if (executorInfo == null) {
      executorInfo = new KrystalExecutorExecutionInfo();
    }
    if (executorServiceTransformer == null) {
      executorServiceTransformer = Function.identity();
    }
  }

  @SuppressWarnings({
    "FieldMayBeFinal", // Lombok needs to set them
    "MismatchedQueryAndUpdateOfCollection" // Lombok reads them
  })
  public static class KrystalExecutorConfigBuilder {

    private Map<String, OutputLogicDecoratorConfig> outputLogicDecoratorConfigs =
        new LinkedHashMap<>();
    private Map<String, KryonDecoratorConfig> kryonDecoratorConfigs = new LinkedHashMap<>();
    private Map<String, DependencyDecoratorConfig> dependencyDecoratorConfigs =
        new LinkedHashMap<>();

    public @This KrystalExecutorConfigBuilder outputLogicDecoratorConfig(
        OutputLogicDecoratorConfig outputLogicDecoratorConfig) {
      this.outputLogicDecoratorConfigs.put(
          outputLogicDecoratorConfig.decoratorType(), outputLogicDecoratorConfig);
      return this;
    }

    public @This KrystalExecutorConfigBuilder kryonDecoratorConfig(
        KryonDecoratorConfig kryonDecoratorConfig) {
      this.kryonDecoratorConfigs.put(kryonDecoratorConfig.decoratorType(), kryonDecoratorConfig);
      return this;
    }

    public @This KrystalExecutorConfigBuilder dependencyDecoratorConfig(
        DependencyDecoratorConfig dependencyDecoratorConfig) {
      this.dependencyDecoratorConfigs.put(
          dependencyDecoratorConfig.decoratorType(), dependencyDecoratorConfig);
      return this;
    }

    public @This KrystalExecutorConfigBuilder configureWith(
        KryonExecutorConfigurator configurator) {
      configurator.addToConfig(this);
      return this;
    }

    public @This KrystalExecutorConfigBuilder traitDispatchDecorator(
        TraitDispatchDecorator traitDispatchDecorator) {
      String decoratorType = traitDispatchDecorator.decoratorType();
      dependencyDecoratorConfigs.put(
          decoratorType,
          new DependencyDecoratorConfig(
              decoratorType,
              dependencyExecutionContext -> {
                Dependency dependency =
                    dependencyExecutionContext.dependentChain().latestDependency();
                if (dependency == null) {
                  return false;
                }
                return dependency.tags().getAnnotationByType(TraitDependency.class).isPresent();
              },
              d -> decoratorType,
              c -> traitDispatchDecorator));
      this.traitDispatchDecorator = traitDispatchDecorator;
      return this;
    }

    public boolean hasOutputLogicDecorator(String decoratorType) {
      return outputLogicDecoratorConfigs.containsKey(decoratorType);
    }

    public boolean hasKryonDecorator(String decoratorType) {
      return kryonDecoratorConfigs.containsKey(decoratorType);
    }
  }
}
