package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public record CallbackBatch(
    KryonId kryonId,
    int dependencyId,
    ImmutableMap<RequestId, Results<?, Object>> resultsByRequest,
    DependantChain dependantChain)
    implements BatchCommand {

  @Override
  public ImmutableSet<RequestId> requestIds() {
    return resultsByRequest().keySet();
  }

  @Override
  public Set<Integer> facetIds() {
    return Set.of(dependencyId);
  }

  public int dependencyId() {
    return dependencyId;
  }
}
