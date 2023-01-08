package com.flipkart.krystal.krystex.node;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class ComputeLogicDefinition<T> extends MainLogicDefinition<T> {

  private final Function<Inputs, T> computeLogic;

  public ComputeLogicDefinition(
      NodeLogicId nodeLogicId, Set<String> inputNames, Function<Inputs, T> nodeLogic) {
    super(nodeLogicId, inputNames);
    this.computeLogic = nodeLogic;
  }

  public ImmutableMap<Inputs, CompletableFuture<T>> execute(ImmutableList<Inputs> inputs) {
    return inputs.stream()
        .collect(
            toImmutableMap(
                Function.identity(), (Inputs t) -> completedFuture(computeLogic.apply(t))));
  }
}
