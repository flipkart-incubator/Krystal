package com.flipkart.krystal.krystex;

import com.flipkart.krystal.core.OutputLogicExecutionInput;
import com.flipkart.krystal.core.OutputLogicExecutionResults;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.NonNull;

@FunctionalInterface
public non-sealed interface OutputLogic<T> extends Logic {
  void execute(OutputLogicExecutionInput input);
}
