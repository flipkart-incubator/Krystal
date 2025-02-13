package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Set;

/**
 * Command created at the client vajram end to send the requests to invoke the server vajram.
 *
 * @param kryonId The dependency kryon Id to execute
 * @param executableRequests The request which need to be executed
 * @param dependantChain The dependant chain leading to this invocation
 * @param skippedRequests The requests which have been skipped by some vajram
 */
public record ForwardSend(
    KryonId kryonId,
    ImmutableMap<RequestId, ? extends Request> executableRequests,
    DependantChain dependantChain,
    ImmutableMap<RequestId, String> skippedRequests)
    implements MultiRequestCommand, ClientSideCommand {

  @Override
  public Set<RequestId> requestIds() {
    return Sets.union(executableRequests().keySet(), skippedRequests().keySet());
  }

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }
}
