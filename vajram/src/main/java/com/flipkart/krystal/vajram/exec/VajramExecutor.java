package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import java.util.concurrent.CompletableFuture;

public interface VajramExecutor extends AutoCloseable {
  <T> CompletableFuture<T> requestExecution(VajramID vajramId, VajramRequest request);
}
