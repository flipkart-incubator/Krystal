package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Set;

/**
 * Command created at the client vajram end to send the requests to invoke the server vajram.
 *
 * @param vajramID The dependency kryon Id to execute
 * @param executableRequests The invocations which need to be executed
 * @param dependentChain The dependant chain leading to this invocation
 * @param skippedInvocations The invocations which have been skipped
 */
public record ForwardSend(
    VajramID vajramID,
    ImmutableMap<InvocationId, Request<?>> executableRequests,
    DependentChain dependentChain,
    ImmutableMap<InvocationId, String> skippedInvocations)
    implements MultiRequestCommand<BatchResponse>, ClientSideCommand<BatchResponse> {

  @Override
  public Set<InvocationId> requestIds() {
    return Sets.union(executableRequests().keySet(), skippedInvocations().keySet());
  }

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }
}
