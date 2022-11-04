package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.krystex.KrystalExecutor;
import java.util.concurrent.CompletableFuture;

public class KrystexVajramExecutor implements VajramExecutor {
  private final VajramRegistry vajramRegistry;
  private final KrystalExecutor krystalExecutor;

  public KrystexVajramExecutor(VajramRegistry vajramRegistry, KrystalExecutor krystalExecutor) {
    this.vajramRegistry = vajramRegistry;
    this.krystalExecutor = krystalExecutor;
  }

  @Override
  public <T> CompletableFuture<T> requestExecution(String vajramId) {
    VajramDefinition vajramDefinition = vajramRegistry.getVajramDefinition(vajramId).orElseThrow();
    vajramRegistry.getExecutionNode(vajramDefinition);
    // TODO integrate with krystal Executor
    return null;
  }
}
