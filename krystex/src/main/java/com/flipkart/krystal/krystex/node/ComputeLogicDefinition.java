package com.flipkart.krystal.krystex.node;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class ComputeLogicDefinition<T> extends MainLogicDefinition<T> {

  private final Function<NodeInputs, T> computeLogic;

  public ComputeLogicDefinition(
      NodeLogicId nodeLogicId, Set<String> inputNames, Function<NodeInputs, T> nodeLogic) {
    super(nodeLogicId, inputNames);
    this.computeLogic = nodeLogic;
  }

  public ImmutableMap<NodeInputs, CompletableFuture<T>> execute(ImmutableList<NodeInputs> inputs) {
    return inputs.stream()
        .collect(
            toImmutableMap(
                Function.identity(), (NodeInputs t) -> completedFuture(computeLogic.apply(t))));
  }
}
