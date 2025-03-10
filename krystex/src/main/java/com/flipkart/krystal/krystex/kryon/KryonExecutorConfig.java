package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecoratorConfig;
import com.flipkart.krystal.krystex.dependencydecorators.TraitDispatchDecorator;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

/**
 * This is used to configure a particular execution of the Krystal graph.
 *
 * @param logicDecorationOrdering The ordering of logic decorators to be applied
 * @param requestScopedLogicDecoratorConfigs The request scoped logic decorators to be applied
 * @param disabledDependantChains Invocation originating for these dependant chains will be blocked.
 *     Useful for avoid graph size explosion in case of recursive dependencies
 * @param kryonExecStrategy Currently on BATCH is supported. More might be added in future
 * @param graphTraversalStrategy DEPTH is more performant and memory efficient. BREADTH is sometimes
 *     useful for debugging
 * @param kryonDecoratorConfigs The request scoped kryon decorators to be applied
 * @param singleThreadExecutor MANDATORY! This is used as the event loop for the message passing
 *     within this execution.
 * @param traitDispatchDecorator used to determine the conformant vajrams bound to traits
 * @param debug If true, more human readable names are give to entities - might be memory
 *     ineffecient.
 * @param _riskyOpenAllKryonsForExternalInvocation DO NOT SET THIS TO TRUE IN PRODUCTION CODE - ELSE
 *     NEW VERSIONS OF CODE CAN BREAK BACKWARD COMPATIBILITY. {@code true} if all vajrams are
 *     allowed to be invoked from outside the krystal graph intead of only allowing vajrams tagged
 *     with @{@link ExternalInvocation}(allow=true)
 */
public record KryonExecutorConfig(
    LogicDecorationOrdering logicDecorationOrdering,
    ImmutableMap<String, List<OutputLogicDecoratorConfig>> requestScopedLogicDecoratorConfigs,
    ImmutableSet<DependantChain> disabledDependantChains,
    KryonExecStrategy kryonExecStrategy,
    GraphTraversalStrategy graphTraversalStrategy,
    @Singular ImmutableMap<String, KryonDecoratorConfig> kryonDecoratorConfigs,
    @Singular ImmutableMap<String, DependencyDecoratorConfig> dependencyDecoratorConfigs,
    @NonNull SingleThreadExecutor singleThreadExecutor,
    TraitDispatchDecorator traitDispatchDecorator,
    boolean enableFlush,
    boolean debug,
    /******* Risky Flags ********/
    boolean _riskyOpenAllKryonsForExternalInvocation) {

  @Builder(toBuilder = true)
  public KryonExecutorConfig {
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
    if (disabledDependantChains == null) {
      disabledDependantChains = ImmutableSet.of();
    }
    if (logicDecorationOrdering == null) {
      logicDecorationOrdering = LogicDecorationOrdering.none();
    }
    if (requestScopedLogicDecoratorConfigs == null) {
      requestScopedLogicDecoratorConfigs = ImmutableMap.of();
    }
    if (traitDispatchDecorator == null) {
      traitDispatchDecorator = DependencyDecorator.NO_OP::decorateDependency;
    }
  }
}
