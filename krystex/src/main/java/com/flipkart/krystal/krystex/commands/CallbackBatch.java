package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.model.DependantChain;
import com.flipkart.krystal.model.KryonId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public record CallbackBatch(
    KryonId kryonId,
    String dependencyName,
    ImmutableMap<RequestId, Results<Object>> resultsByRequest,
    DependantChain dependantChain)
    implements BatchCommand {

  @Override
  public ImmutableSet<RequestId> requestIds() {
    return resultsByRequest().keySet();
  }

  @Override
  public Set<String> inputNames() {
    return Set.of(dependencyName);
  }

  public String dependencyName() {
    return dependencyName;
  }
}
