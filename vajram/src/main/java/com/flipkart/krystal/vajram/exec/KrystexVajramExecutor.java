package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.NodeDefinition;
import java.util.concurrent.CompletableFuture;

public class KrystexVajramExecutor implements VajramExecutor {
  private final VajramGraph vajramGraph;
  private final KrystalExecutor krystalExecutor;

  public KrystexVajramExecutor(VajramGraph vajramGraph, KrystalExecutor krystalExecutor) {
    this.vajramGraph = vajramGraph;
    this.krystalExecutor = krystalExecutor;
  }

  @Override
  public <T> CompletableFuture<T> requestExecution(String vajramId) {
    NodeDefinition<T> nodeToExecute =
        vajramGraph.<T>createVajramDAG(vajramId).vajramLogicNodeDefinition();
    return krystalExecutor
        .requestExecution(nodeToExecute)
        .getAllResults()
        .thenApply(
            results -> {
              if (results.size() != 1) {
                // This should never happen
                throw new AssertionError("Received incorrect number of results.");
              }
              return results.iterator().next();
            });
  }
}
