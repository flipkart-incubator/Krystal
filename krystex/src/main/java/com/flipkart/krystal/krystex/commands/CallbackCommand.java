package com.flipkart.krystal.krystex.commands;

import static java.util.Collections.unmodifiableMap;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.DepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.request.InvocationId;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public record CallbackCommand(
    VajramID vajramID,
    Dependency dependency,
    Map<InvocationId, DepResponse<Request<@Nullable Object>, @Nullable Object>> resultsByRequest,
    DependentChain dependentChain)
    implements MultiRequestCommand<BatchResponse> {

  public CallbackCommand {
    resultsByRequest = unmodifiableMap(resultsByRequest);
  }

  @Override
  public Set<? extends InvocationId> invocationIds() {
    return resultsByRequest.keySet();
  }
}
