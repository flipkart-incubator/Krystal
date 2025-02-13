package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.DepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public record CallbackCommand(
    KryonId kryonId,
    Dependency dependency,
    ImmutableMap<RequestId, DepResponse<Request<Object>, Object>> resultsByRequest,
    DependantChain dependantChain)
    implements MultiRequestCommand {

  @Override
  public ImmutableSet<RequestId> requestIds() {
    return resultsByRequest().keySet();
  }

  public Set<Facet> facets() {
    return Set.of(dependency);
  }

  public Dependency dependency() {
    return dependency;
  }
}
