package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.vajramexecutor.krystex.Utils.toNodeInputs;

import com.flipkart.krystal.krystex.KrystalNodeExecutor;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.exec.VajramExecutor;
import com.flipkart.krystal.vajram.inputs.InputValues;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class KrystexVajramExecutor<C extends ApplicationRequestContext>
    implements VajramExecutor<C> {
  private final VajramGraph vajramGraph;
  private final String requestId;
  private final C applicationRequestContext;
  private final KrystalNodeExecutor krystalExecutor;

  public KrystexVajramExecutor(
      VajramGraph vajramGraph, String requestId, C applicationRequestContext) {
    this.vajramGraph = vajramGraph;
    this.requestId = requestId;
    this.applicationRequestContext = applicationRequestContext;
    this.krystalExecutor =
        new KrystalNodeExecutor(vajramGraph.getClusterDefinitionRegistry(), requestId);
  }

  @Override
  public <T> CompletableFuture<T> execute(
      VajramID vajramId, Function<C, VajramRequest> vajramRequestBuilder) {
    return krystalExecutor.executeNode(
        vajramGraph.getExecutable(vajramId),
        toNodeInputs(
            new InputValues(vajramRequestBuilder.apply(applicationRequestContext).asMap())),
        new RequestId(requestId));
  }

  @Override
  public void close() {
    krystalExecutor.close();
  }
}
