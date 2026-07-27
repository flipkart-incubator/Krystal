package com.flipkart.krystal.krystex.commands;

import static java.util.Collections.unmodifiableSet;

import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.request.RequestId;
import java.util.Map;
import java.util.Set;

public record CallbackBatch(
    KryonId kryonId,
    String dependencyName,
    Map<RequestId, Results<Object>> resultsByRequest,
    DependantChain dependantChain)
    implements BatchCommand {

  @Override
  public Set<RequestId> requestIds() {
    return unmodifiableSet(resultsByRequest().keySet());
  }
}
