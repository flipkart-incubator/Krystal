package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.krystex.KrystalNodeExecutor;
import com.flipkart.krystal.krystex.NodeInputs;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class KrystexVajramExecutor<C extends ApplicationRequestContext>
    implements VajramExecutor<C> {
  private final VajramGraph<C> vajramGraph;
  private final KrystalNodeExecutor krystalExecutor;

  public KrystexVajramExecutor(
      VajramGraph<C> vajramGraph, String requestId, C applicationRequestContext) {
    this.vajramGraph = vajramGraph;
    this.krystalExecutor =
        new KrystalNodeExecutor(vajramGraph.getNodeDefinitionRegistry(), requestId);
    krystalExecutor.provideInputsAndMarkDone(
        vajramGraph.getApplicationContextProviderNodeId(),
        new NodeInputs(
            ImmutableMap.of(
                VajramGraph.APPLICATION_REQUEST_CONTEXT_KEY, applicationRequestContext)));
  }

  @Override
  public <T> CompletableFuture<T> execute(
      VajramID vajramId, Function<C, VajramRequest> vajramRequestBuilder) {
    VajramDAG<T> vajramDAG = vajramGraph.createVajramDAG(vajramId, vajramRequestBuilder);

    return krystalExecutor
        .execute(vajramDAG.vajramLogicNodeDefinition())
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

  @Override
  public void close() {
    krystalExecutor.close();
  }
}
