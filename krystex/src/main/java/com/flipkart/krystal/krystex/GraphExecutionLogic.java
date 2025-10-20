package com.flipkart.krystal.krystex;

import com.flipkart.krystal.core.GraphExecutionData;

@FunctionalInterface
public non-sealed interface GraphExecutionLogic extends Logic {
  void executeGraph(GraphExecutionData _graphExecData);
}
