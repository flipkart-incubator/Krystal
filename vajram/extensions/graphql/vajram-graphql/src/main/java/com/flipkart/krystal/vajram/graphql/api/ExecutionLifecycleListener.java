package com.flipkart.krystal.vajram.graphql.api;

import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import graphql.execution.ExecutionContext;

public interface ExecutionLifecycleListener {
  void onExecutionStart(ExecutionStartEvent krystexVajramExecutor);

  record ExecutionStartEvent(KrystexVajramExecutor executor, ExecutionContext executionContext) {}
}
