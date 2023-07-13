package com.flipkart.krystal.krystex.commands;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;

public record ForwardBatchCommand(
    NodeId nodeId,
    ImmutableMap<RequestId, Inputs> inputsByRequest,
    DependantChain dependantChain,
    ImmutableMap<RequestId, String> skipReasonsByRequest,
    Optional<String> skipReason)
    implements BatchNodeCommand {

  public ForwardBatchCommand {
    if (skipReason.isEmpty() && inputsByRequest.isEmpty() && !skipReasonsByRequest.isEmpty()) {
      skipReason =
          Optional.of(skipReasonsByRequest.values().stream().collect(joining(lineSeparator())));
    }
  }

  public ForwardBatchCommand(
      NodeId nodeId,
      ImmutableMap<RequestId, Inputs> inputsByRequest,
      ImmutableMap<RequestId, String> skipReasonsByRequest,
      DependantChain dependantChain) {
    this(nodeId, inputsByRequest, dependantChain, skipReasonsByRequest, Optional.empty());
  }

  @Override
  public Set<RequestId> requestIds() {
    return Sets.union(inputsByRequest().keySet(), skipReasonsByRequest().keySet());
  }
}
