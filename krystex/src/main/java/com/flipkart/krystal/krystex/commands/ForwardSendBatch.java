package com.flipkart.krystal.krystex.commands;

import static java.util.Collections.unmodifiableMap;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;

/**
 * Command created at the client vajram end to send the requests to invoke the server vajram.
 *
 * @param vajramID The dependency kryon Id to execute
 * @param executableRequests The invocations which need to be executed
 * @param dependentChain The dependant chain leading to this invocation
 * @param skippedInvocations The invocations which have been skipped
 */
public record ForwardSendBatch(
    VajramID vajramID,
    Map<InvocationId, Request<Object>> executableRequests,
    DependentChain dependentChain,
    Map<InvocationId, String> skippedInvocations)
    implements MultiRequestCommand<BatchResponse>, ClientSideCommand<BatchResponse> {

  /**
   * @param vajramID
   * @param executableRequests Must not be mutated after passing to this constructor
   * @param dependentChain
   * @param skippedInvocations Must not be mutated after passing to this constructor
   */
  public ForwardSendBatch {
    executableRequests = unmodifiableMap(executableRequests);
    skippedInvocations = unmodifiableMap(skippedInvocations);
  }

  @Override
  public Set<InvocationId> invocationIds() {
    return Sets.union(executableRequests().keySet(), skippedInvocations().keySet());
  }

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }
}
