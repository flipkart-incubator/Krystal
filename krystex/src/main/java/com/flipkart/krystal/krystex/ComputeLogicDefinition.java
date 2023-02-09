package com.flipkart.krystal.krystex;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.logic.LogicTag;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class ComputeLogicDefinition<T> extends MainLogicDefinition<T> {

  private final Function<Inputs, ValueOrError<T>> computeLogic;

  public ComputeLogicDefinition(
      NodeLogicId nodeLogicId,
      Set<String> inputNames,
      Function<Inputs, ValueOrError<T>> nodeLogic,
      ImmutableMap<String, LogicTag> logicTags) {
    super(nodeLogicId, inputNames, logicTags);
    this.computeLogic = nodeLogic;
  }

  public ImmutableMap<Inputs, CompletableFuture<T>> execute(ImmutableList<Inputs> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(Function.identity(), inputs -> computeLogic.apply(inputs).toFuture()));
  }
}
