package com.flipkart.krystal.krystex.commands;

import static java.util.Collections.unmodifiableMap;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.request.InvocationId;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.KeyFor;

public record ForwardReceiveBatch(
    VajramID vajramID,
    Map<InvocationId, FacetValues> executableInvocations,
    DependentChain dependentChain)
    implements MultiRequestCommand<KryonCommandResponse>, ServerSideCommand<KryonCommandResponse> {

  /**
   * @param vajramID
   * @param executableInvocations Must not be mutated after passing to this constructor
   * @param dependentChain
   */
  public ForwardReceiveBatch {
    executableInvocations = unmodifiableMap(executableInvocations);
  }

  @Override
  public Set<@KeyFor("this.executableInvocations()") InvocationId> invocationIds() {
    return executableInvocations().keySet();
  }

  public boolean shouldSkip() {
    return executableInvocations.isEmpty();
  }
}
