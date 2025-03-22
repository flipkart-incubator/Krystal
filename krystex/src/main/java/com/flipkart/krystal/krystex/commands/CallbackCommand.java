package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.DepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.krystex.kryon.BatchResponse;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public record CallbackCommand(
    VajramID vajramID,
    Dependency dependency,
    ImmutableMap<InvocationId, DepResponse<Request<Object>, Object>> resultsByRequest,
    DependentChain dependentChain)
    implements MultiRequestCommand<BatchResponse> {

  @Override
  public ImmutableSet<InvocationId> requestIds() {
    return resultsByRequest().keySet();
  }

  public Set<Facet> facets() {
    return Set.of(dependency);
  }

  public Dependency dependency() {
    return dependency;
  }
}
