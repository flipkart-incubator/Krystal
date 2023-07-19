package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Set;

public record ForwardBatchCommand(
    NodeId nodeId,
    ImmutableSet<String> inputNames,
    ImmutableMap<RequestId, Inputs> executableRequests,
    DependantChain dependantChain,
    ImmutableMap<RequestId, String> skippedRequests)
    implements BatchNodeCommand {

  @Override
  public Set<RequestId> requestIds() {
    return Sets.union(executableRequests().keySet(), skippedRequests().keySet());
  }

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }
}
