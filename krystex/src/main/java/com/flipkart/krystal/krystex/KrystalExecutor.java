package com.flipkart.krystal.krystex;

public interface KrystalExecutor {

  <T> Result<T> requestExecution(NodeDefinition<T> nodeDefinition);
}
