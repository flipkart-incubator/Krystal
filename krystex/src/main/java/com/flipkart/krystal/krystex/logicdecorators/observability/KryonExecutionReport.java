package com.flipkart.krystal.krystex.logicdecorators.observability;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableList;

public sealed interface KryonExecutionReport permits DefaultKryonExecutionReport {

  void reportMainLogicStart(
      KryonId kryonId, KryonLogicId kryonLogicId, ImmutableList<? extends FacetValues> inputs);

  void reportMainLogicEnd(
      KryonId kryonId, KryonLogicId kryonLogicId, LogicExecResults logicExecResults);
}
