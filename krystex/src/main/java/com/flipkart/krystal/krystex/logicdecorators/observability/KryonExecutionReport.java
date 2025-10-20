package com.flipkart.krystal.krystex.logicdecorators.observability;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import java.util.List;

public sealed interface KryonExecutionReport permits DefaultKryonExecutionReport {

  void reportMainLogicStart(
      VajramID vajramID, KryonLogicId kryonLogicId, List<? extends ExecutionItem> inputs);

  void reportMainLogicEnd(
      VajramID vajramID, KryonLogicId kryonLogicId, LogicExecResults logicExecResults);
}
