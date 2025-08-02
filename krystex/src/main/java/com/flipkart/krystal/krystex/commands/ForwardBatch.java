package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;

public record ForwardBatch(
    KryonId kryonId,
    Map<RequestId, Facets> executableRequests,
    DependantChain dependantChain,
    Map<RequestId, String> skippedRequests)
    implements BatchCommand {

  @Override
  public Set<RequestId> requestIds() {
    return Sets.union(executableRequests().keySet(), skippedRequests().keySet());
  }

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }
}
