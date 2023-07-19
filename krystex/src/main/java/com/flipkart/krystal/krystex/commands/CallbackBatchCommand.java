package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public record CallbackBatchCommand(
    NodeId nodeId,
    String dependencyName,
    ImmutableMap<RequestId, Results<Object>> resultsByRequest,
    DependantChain dependantChain)
    implements BatchNodeCommand {

  @Override
  public ImmutableSet<RequestId> requestIds() {
    return resultsByRequest().keySet();
  }

  @Override
  public Set<String> inputNames() {
    return Set.of(dependencyName);
  }

  public String dependencyName() {
    return dependencyName;
  }
}
