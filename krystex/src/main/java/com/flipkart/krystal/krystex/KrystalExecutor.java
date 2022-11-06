package com.flipkart.krystal.krystex;

public interface KrystalExecutor {
  <T> Node<T> requestExecution(NodeDefinition<T> nodeDefinition);
}
