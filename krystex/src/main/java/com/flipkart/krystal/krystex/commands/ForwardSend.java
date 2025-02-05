package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Set;

/**
 * Command created at the client vajram end
 *
 * @param kryonId
 * @param executableRequests
 * @param dependantChain
 * @param skippedRequests
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
