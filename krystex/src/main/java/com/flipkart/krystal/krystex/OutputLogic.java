package com.flipkart.krystal.krystex;

import com.flipkart.krystal.core.OutputLogicExecutionInput;

@FunctionalInterface
public non-sealed interface OutputLogic<T> extends Logic {
  void execute(OutputLogicExecutionInput input);
}
