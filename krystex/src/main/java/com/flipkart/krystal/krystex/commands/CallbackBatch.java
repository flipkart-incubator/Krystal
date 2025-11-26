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

public record CallbackBatch(
    VajramID vajramID,
    Dependency dependency,
    Map<InvocationId, DepResponse<Request<Object>, Object>> resultsByRequest,
    DependentChain dependentChain)
    implements MultiRequestCommand<BatchResponse> {

  public CallbackBatch {
    resultsByRequest = unmodifiableMap(resultsByRequest);
  }

  @Override
  public Set<? extends InvocationId> invocationIds() {
    return resultsByRequest.keySet();
  }
}
