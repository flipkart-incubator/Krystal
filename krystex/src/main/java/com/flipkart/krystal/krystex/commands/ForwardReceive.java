package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Set;

public record ForwardReceive(
    VajramID vajramID,
    ImmutableMap<RequestId, ? extends FacetValues> executableRequests,
    DependantChain dependantChain,
    ImmutableMap<RequestId, String> skippedRequests)
    implements MultiRequestCommand, ServerSideCommand {

  @Override
  public Set<RequestId> requestIds() {
    return Sets.union(executableRequests().keySet(), skippedRequests().keySet());
  }

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }
}
