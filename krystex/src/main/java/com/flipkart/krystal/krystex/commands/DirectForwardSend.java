package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.DirectResponse;
import com.flipkart.krystal.krystex.request.InvocationId;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public record DirectForwardSend(
    VajramID vajramID,
    List<RequestResponseFuture<Request<Object>, Object>> executableRequests,
    DependentChain dependentChain)
    implements MultiRequestDirectCommand<DirectResponse>, ClientSideCommand<DirectResponse> {

  public boolean shouldSkip() {
    return executableRequests.isEmpty();
  }
}
