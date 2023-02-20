package com.flipkart.krystal.krystex;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.logic.LogicTag;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class ComputeLogicDefinition<T> extends MainLogicDefinition<T> {

  private final Function<ImmutableList<Inputs>, ImmutableMap<Inputs, ValueOrError<T>>> computeLogic;

  public ComputeLogicDefinition(
      NodeLogicId nodeLogicId,
      Set<String> inputNames,
      Function<ImmutableList<Inputs>, ImmutableMap<Inputs, ValueOrError<T>>> nodeLogic,
      ImmutableMap<String, LogicTag> logicTags) {
    super(nodeLogicId, inputNames, logicTags);
    this.computeLogic = nodeLogic;
  }

  public ImmutableMap<Inputs, CompletableFuture<T>> execute(ImmutableList<Inputs> inputsList) {
    return computeLogic.apply(inputsList).entrySet().stream()
        .collect(toImmutableMap(Entry::getKey, e -> e.getValue().toFuture()));
  }
}
