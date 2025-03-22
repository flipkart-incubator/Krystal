package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Set;

public record ForwardReceive(
    VajramID vajramID,
    ImmutableMap<InvocationId, ? extends FacetValues> executableRequests,
    DependentChain dependentChain,
    ImmutableMap<InvocationId, String> skippedRequests)
    implements MultiRequestCommand<KryonCommandResponse>, ServerSideCommand<KryonCommandResponse> {

  @Override
  public Set<InvocationId> requestIds() {
    return Sets.union(executableRequests().keySet(), skippedRequests().keySet());
  }

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }
}
