package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class IOLogicDefinition<T> extends MainLogicDefinition<T> {

  private final MainLogic<T> nodeLogic;

  public IOLogicDefinition(NodeLogicId nodeLogicId, Set<String> inputs, MainLogic<T> nodeLogic) {
    super(nodeLogicId, inputs);
    this.nodeLogic = nodeLogic;
  }

  public ImmutableMap<Inputs, CompletableFuture<T>> execute(ImmutableList<Inputs> inputs) {
    return nodeLogic.execute(inputs);
  }
}
