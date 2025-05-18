package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecoratorConfig;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.jetbrains.annotations.TestOnly;

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
 * @param executor MANDATORY! This is used as the event loop for the message passing within this
 *     execution.
 * @param traitDispatchDecorator used to determine the conformant vajrams bound to traits
 * @param debug If true, more human-readable names are give to entities - might be memory
 *     ineffecient.
 * @param _riskyOpenAllKryonsForExternalInvocation DO NOT SET THIS TO TRUE IN PRODUCTION CODE - ELSE
 *     NEW VERSIONS OF CODE CAN BREAK BACKWARD COMPATIBILITY. TO BE USED IN TESTING CODE ONLY.
 *     {@code true} if all vajrams are allowed to be invoked from outside the krystal graph instead
 *     of only allowing vajrams tagged with @{@link ExternallyInvocable}(allow=true)
 */
public record KryonExecutorConfig(
    String executorId,
    DecorationOrdering decorationOrdering,
    ImmutableSet<DependentChain> disabledDependentChains,
    KryonExecStrategy kryonExecStrategy,
    GraphTraversalStrategy graphTraversalStrategy,
    @Singular ImmutableMap<String, OutputLogicDecoratorConfig> outputLogicDecoratorConfigs,
    @Singular ImmutableMap<String, KryonDecoratorConfig> kryonDecoratorConfigs,
    @Singular ImmutableMap<String, DependencyDecoratorConfig> dependencyDecoratorConfigs,
    @NonNull SingleThreadExecutor executor,
    TraitDispatchDecorator traitDispatchDecorator,
    boolean debug,
    /* ****** Risky Flags ********/
    @TestOnly @Deprecated boolean _riskyOpenAllKryonsForExternalInvocation) {
  private static final AtomicLong EXEC_COUNT = new AtomicLong();

  @Builder(toBuilder = true)
  public KryonExecutorConfig {
    if (executorId == null) {
      executorId = "KrystalExecutor-" + EXEC_COUNT.getAndIncrement();
    }
    if (kryonExecStrategy == null) {
      kryonExecStrategy = BATCH;
    }
    if (graphTraversalStrategy == null) {
      graphTraversalStrategy = DEPTH;
    }
    if (kryonDecoratorConfigs == null) {
      kryonDecoratorConfigs = ImmutableMap.of();
    }
    if (dependencyDecoratorConfigs == null) {
      dependencyDecoratorConfigs = ImmutableMap.of();
    }
    if (disabledDependentChains == null) {
      disabledDependentChains = ImmutableSet.of();
    }
    if (decorationOrdering == null) {
      decorationOrdering = DecorationOrdering.none();
    }
    if (outputLogicDecoratorConfigs == null) {
      outputLogicDecoratorConfigs = ImmutableMap.of();
    }
    if (traitDispatchDecorator == null) {
      traitDispatchDecorator = DependencyDecorator.NO_OP::decorateDependency;
    }
  }

  public static class KryonExecutorConfigBuilder {

    public @This KryonExecutorConfigBuilder configureWith(KryonExecutorConfigurator applier) {
      applier.addToConfig(this);
      return this;
    }
  }
}
