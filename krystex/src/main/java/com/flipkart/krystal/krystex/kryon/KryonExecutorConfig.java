package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

/**
 * @param logicDecorationOrdering
 * @param requestScopedLogicDecoratorConfigs
 * @param disabledDependantChains
 * @param kryonExecStrategy
 * @param graphTraversalStrategy
 * @param requestScopedKryonDecoratorConfigs
 * @param singleThreadExecutor
 * @param debug
 * @param _riskyOpenAllKryonsForExternalInvocation DO NOT SET THIS TO TRUE IN PRODUCTION CODE - ELSE
 *     NEW VERSIONS OF CODE CAN BREAK BACKWARD COMPATIBILITY. {@code true} if all vajrams are
 *     allowed to be invoked from outside the krystal graph intead of only allowing vajrams tagged
 *     with @{@link ExternalInvocation}(allow=true)
 */
public record KryonExecutorConfig(
    LogicDecorationOrdering logicDecorationOrdering,
    Map<String, List<OutputLogicDecoratorConfig>> requestScopedLogicDecoratorConfigs,
    ImmutableSet<DependantChain> disabledDependantChains,
    KryonExecStrategy kryonExecStrategy,
    GraphTraversalStrategy graphTraversalStrategy,
    @Singular Map<String, KryonDecoratorConfig> requestScopedKryonDecoratorConfigs,
    @NonNull SingleThreadExecutor singleThreadExecutor,
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
    if (requestScopedKryonDecoratorConfigs == null) {
      requestScopedKryonDecoratorConfigs = Map.of();
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
  }
}
