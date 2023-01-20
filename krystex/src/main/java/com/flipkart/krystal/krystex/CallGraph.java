package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.node.NodeId;
import com.google.common.collect.ImmutableList;

public record CallGraph(NodeId nodeId, ImmutableList<CallGraph> previousCalls) {
  private static final CallGraph EMPTY = new CallGraph(null, ImmutableList.of());

  public static CallGraph empty() {
    return EMPTY;
  }
}
