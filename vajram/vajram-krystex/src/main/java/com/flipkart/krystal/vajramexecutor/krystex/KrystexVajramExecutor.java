package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.vajramexecutor.krystex.Utils.toNodeInputs;

import com.flipkart.krystal.krystex.node.KrystalNodeExecutor;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.exec.VajramExecutor;
import com.flipkart.krystal.vajram.inputs.InputValues;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class KrystexVajramExecutor<C extends ApplicationRequestContext>
    implements VajramExecutor<C> {
  private final VajramNodeGraph vajramNodeGraph;
  private final C applicationRequestContext;
  private final KrystalNodeExecutor krystalExecutor;

  KrystexVajramExecutor(
      VajramNodeGraph vajramNodeGraph, C applicationRequestContext) {
    this.vajramNodeGraph = vajramNodeGraph;
    this.applicationRequestContext = applicationRequestContext;
    this.krystalExecutor =
        new KrystalNodeExecutor(
            vajramNodeGraph.getNodeDefinitionRegistry(), applicationRequestContext.requestId());
  }

  @Override
  public <T> CompletableFuture<T> execute(
      VajramID vajramId, Function<C, VajramRequest> vajramRequestBuilder) {
    return krystalExecutor.executeNode(
        vajramNodeGraph.getNodeId(vajramId),
        toNodeInputs(
            new InputValues(vajramRequestBuilder.apply(applicationRequestContext).asMap())));
  }

  @Override
  public void close() {
    krystalExecutor.close();
  }
}
