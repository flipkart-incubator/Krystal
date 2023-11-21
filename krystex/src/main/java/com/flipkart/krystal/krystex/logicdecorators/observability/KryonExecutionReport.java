package com.flipkart.krystal.krystex.logicdecorators.observability;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableList;

public sealed interface KryonExecutionReport permits DefaultKryonExecutionReport {

  void reportMainLogicStart(
      KryonId kryonId, KryonLogicId kryonLogicId, ImmutableList<Inputs> inputs);

  void reportMainLogicEnd(KryonId kryonId, KryonLogicId kryonLogicId, Results<Object> results);
}
