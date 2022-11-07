package com.flipkart.krystal.krystex;

public interface KrystalExecutor extends AutoCloseable{
  <T> Node<T> execute(NodeDefinition<T> nodeDefinition);
}
