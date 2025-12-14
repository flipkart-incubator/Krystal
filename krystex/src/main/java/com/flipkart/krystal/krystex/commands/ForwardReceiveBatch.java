package com.flipkart.krystal.krystex.commands;

import static java.util.Collections.unmodifiableMap;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;

public record ForwardReceiveBatch(
    VajramID vajramID,
    Map<InvocationId, FacetValues> executableInvocations,
    DependentChain dependentChain,
    Map<InvocationId, String> invocationsToSkip)
    implements MultiRequestCommand<BatchResponse>, ServerSideCommand<BatchResponse> {

  /**
   * @param vajramID
   * @param executableInvocations Must not be mutated after passing to this constructor
   * @param dependentChain
   * @param invocationsToSkip Must not be mutated after passing to this constructor
   */
  public ForwardReceiveBatch {
    executableInvocations = unmodifiableMap(executableInvocations);
    invocationsToSkip = unmodifiableMap(invocationsToSkip);
  }

  @Override
  public Set<InvocationId> invocationIds() {
    return Sets.union(executableInvocations().keySet(), invocationsToSkip().keySet());
  }

  public boolean shouldSkip() {
    return executableInvocations.isEmpty();
  }
}
