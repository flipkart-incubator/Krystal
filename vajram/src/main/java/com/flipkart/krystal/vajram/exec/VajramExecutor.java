package com.flipkart.krystal.vajram.exec;

import java.util.concurrent.CompletableFuture;

public interface VajramExecutor {
  <T> CompletableFuture<T> requestExecution(String vajramId);
}
