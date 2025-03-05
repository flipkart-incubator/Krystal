package com.flipkart.krystal.krystex.logicdecorators.observability;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.google.common.collect.ImmutableList;

public sealed interface KryonExecutionReport permits DefaultKryonExecutionReport {

  void reportMainLogicStart(
      VajramID vajramID, KryonLogicId kryonLogicId, ImmutableList<? extends FacetValues> inputs);

  void reportMainLogicEnd(
      VajramID vajramID, KryonLogicId kryonLogicId, LogicExecResults logicExecResults);
}
