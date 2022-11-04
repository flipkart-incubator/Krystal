package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableList;

public interface KrystalExecutor {

  <T> ImmutableList<Result<T>> requestExecution(NodeDefinition<T> nodeDefinition);
}
