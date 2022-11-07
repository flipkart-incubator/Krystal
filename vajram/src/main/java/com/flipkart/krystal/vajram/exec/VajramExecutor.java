package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.VajramRequest;
import java.util.concurrent.CompletableFuture;

public interface VajramExecutor extends AutoCloseable {
  <T> CompletableFuture<T> requestExecution(String vajramId, VajramRequest request);
}
