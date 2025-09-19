package com.flipkart.krystal.krystex;

import com.flipkart.krystal.core.GraphExecutionData;
import com.flipkart.krystal.core.OutputLogicExecutionInput;
import com.flipkart.krystal.core.OutputLogicExecutionResults;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@FunctionalInterface
public non-sealed interface GraphExecutionLogic extends Logic {
  void executeGraph(GraphExecutionData _graphExecData);
}
