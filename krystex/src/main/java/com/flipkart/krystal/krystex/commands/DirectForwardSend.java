package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.DirectResponse;
import java.util.List;

public record DirectForwardSend(
    VajramID vajramID,
    List<? extends RequestResponseFuture<? extends Request<?>, ?>> executableRequests,
    DependentChain dependentChain)
    implements MultiRequestDirectCommand<DirectResponse>, ClientSideCommand<DirectResponse> {

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }
}
