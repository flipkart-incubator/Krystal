package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.VajramRequest;
import com.google.common.collect.ImmutableMap;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface VajramExecutor extends AutoCloseable {
  <T> CompletableFuture<T> requestExecution(String vajramId, VajramRequest request);
  <T> CompletableFuture<T> requestExecutionWithInputs(String vajramId, ImmutableMap<String, Optional<Object>> inputs);
}
