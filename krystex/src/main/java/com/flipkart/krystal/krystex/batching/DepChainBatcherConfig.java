package com.flipkart.krystal.krystex.batching;

import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.vajram.batching.InputBatcher;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public record DepChainBatcherConfig(
    Set<DependentChain> dependentChains,
    Function<LogicExecutionContext, String> instanceIdGenerator) {

  static DepChainBatcherConfig sharedBatcher(String instanceId, DependentChain... dependentChains) {
    return sharedBatcher(instanceId, ImmutableSet.copyOf(dependentChains));
  }

  static DepChainBatcherConfig sharedBatcher(
      String instanceId, ImmutableSet<DependentChain> dependentChains) {
    return new DepChainBatcherConfig(dependentChains, logicExecutionContext -> instanceId);
  }

  public static DepChainBatcherConfig simple(Supplier<InputBatcher> inputBatcherSupplier) {
    return new DepChainBatcherConfig(
        Set.of(), logicExecutionContext -> logicExecutionContext.vajramID().id());
  }
}
