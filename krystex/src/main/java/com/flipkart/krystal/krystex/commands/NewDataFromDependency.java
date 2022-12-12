package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.MultiResult;
import com.flipkart.krystal.krystex.Node;
import com.flipkart.krystal.krystex.SingleResult;
import com.google.common.collect.ImmutableCollection;

public record NewDataFromDependency(
    Node<?> node,
    String dependencyNodeId,
    ImmutableCollection<SingleResult<?>> newData,
    MultiResult<?> result)
    implements NodeCommand {

  public NewDataFromDependency(
      Node<?> node, String dependencyNodeId, ImmutableCollection<SingleResult<?>> newData) {
    this(node, dependencyNodeId, newData, new MultiResult<>());
  }
}
