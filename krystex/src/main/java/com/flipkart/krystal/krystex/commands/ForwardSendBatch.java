package com.flipkart.krystal.krystex.commands;

import static java.util.Collections.unmodifiableMap;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.request.InvocationId;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.KeyFor;

/**
 * Command created at the client vajram end to send the requests to invoke the server vajram.
 *
 * @param vajramID The dependency kryon Id to execute
 * @param executableRequests The invocations which need to be executed
 * @param dependentChain The dependant chain leading to this invocation
 */
public record ForwardSendBatch(
    VajramID vajramID,
    Map<InvocationId, Request<Object>> executableRequests,
    DependentChain dependentChain)
    implements MultiRequestCommand<BatchResponse>, ClientSideCommand<BatchResponse> {

  /**
   * @param vajramID
   * @param executableRequests Must not be mutated after passing to this constructor
   * @param dependentChain
   */
  public ForwardSendBatch {
    executableRequests = unmodifiableMap(executableRequests);
  }

  @Override
  public Set<@KeyFor("this.executableRequests()") InvocationId> invocationIds() {
    return executableRequests().keySet();
  }

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }

  @Override
  public ForwardSendBatch rerouteTo(VajramID targetVajramID) {
    return new ForwardSendBatch(targetVajramID, executableRequests(), dependentChain());
  }
}
