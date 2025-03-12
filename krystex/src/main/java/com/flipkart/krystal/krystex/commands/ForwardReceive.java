package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Set;

public record ForwardReceive(
    VajramID vajramID,
    ImmutableMap<RequestId, ? extends FacetValues> executableRequests,
    DependentChain dependentChain,
    ImmutableMap<RequestId, String> skippedRequests)
    implements MultiRequestCommand<KryonResponse>, ServerSideCommand<KryonResponse> {

  @Override
  public Set<RequestId> requestIds() {
    return Sets.union(executableRequests().keySet(), skippedRequests().keySet());
  }

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }
}
