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
    ImmutableMap<InvocationId, ? extends FacetValues> executableInvocations,
    DependentChain dependentChain,
    ImmutableMap<InvocationId, String> invocationsToSkip)
    implements MultiRequestCommand<KryonCommandResponse>, ServerSideCommand<KryonCommandResponse> {

  @Override
  public Set<InvocationId> invocationIds() {
    return Sets.union(executableInvocations().keySet(), invocationsToSkip().keySet());
  }

  public boolean shouldSkip() {
    return executableInvocations.isEmpty();
  }
}
