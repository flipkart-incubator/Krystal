package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;

public record ForwardReceive(
    VajramID vajramID,
    ImmutableMap<InvocationId, FacetValues> executableInvocations,
    DependentChain dependentChain,
    ImmutableMap<InvocationId, String> invocationsToSkip)
    implements MultiRequestCommand<KryonCommandResponse>, ServerSideCommand<KryonCommandResponse> {

  /**
   * @param vajramID
   * @param executableInvocations Must not be mutated after passing to this constructor
   * @param dependentChain
   * @param invocationsToSkip Must not be mutated after passing to this constructor
   */
  public ForwardReceive {
    executableInvocations = ImmutableMap.copyOf(executableInvocations);
    invocationsToSkip = ImmutableMap.copyOf(invocationsToSkip);
  }

  public ForwardReceive (VajramID vajramID,
      Map<InvocationId,FacetValues> executableRequests,
      DependentChain dependentChain,
      Map<InvocationId, String> skippedInvocations) {
    this(
        vajramID,
        ImmutableMap.copyOf(executableRequests),
        dependentChain,
        ImmutableMap.copyOf(skippedInvocations));
  }

  @Override
  public Set<InvocationId> invocationIds() {
    return Sets.union(executableInvocations().keySet(), invocationsToSkip().keySet());
  }

  public boolean shouldSkip() {
    return executableInvocations.isEmpty();
  }
}
