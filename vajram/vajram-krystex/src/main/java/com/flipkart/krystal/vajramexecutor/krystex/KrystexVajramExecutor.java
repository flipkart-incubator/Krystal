package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutor;
import com.flipkart.krystal.utils.MultiLeasePool;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.exec.VajramExecutor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class KrystexVajramExecutor<C extends ApplicationRequestContext>
    implements VajramExecutor<C> {

  private final VajramNodeGraph vajramNodeGraph;
  private final C applicationRequestContext;
  private final KrystalExecutor krystalExecutor;

  public KrystexVajramExecutor(
      VajramNodeGraph vajramNodeGraph,
      LogicDecorationOrdering logicDecorationOrdering,
      MultiLeasePool<? extends ExecutorService> executorServicePool,
      C applicationRequestContext,
      Map<String, List<MainLogicDecoratorConfig>> requestScopedLogicDecoratorConfigs) {
    this.vajramNodeGraph = vajramNodeGraph;
    this.applicationRequestContext = applicationRequestContext;
    this.krystalExecutor =
        new KrystalNodeExecutor(
            vajramNodeGraph.getNodeDefinitionRegistry(),
            logicDecorationOrdering,
            executorServicePool,
            applicationRequestContext.requestId(),
            requestScopedLogicDecoratorConfigs);
  }

  @Override
  public <T> CompletableFuture<T> execute(
      VajramID vajramId, Function<C, VajramRequest> vajramRequestBuilder) {
    return krystalExecutor.executeNode(
        vajramNodeGraph.getNodeId(vajramId),
        vajramRequestBuilder.apply(applicationRequestContext).toInputValues());
  }

  @Override
  public <T> CompletableFuture<T> execute(
      VajramID vajramId, Function<C, VajramRequest> vajramRequestBuilder, String requestId) {
    return krystalExecutor.executeNode(
        vajramNodeGraph.getNodeId(vajramId),
        vajramRequestBuilder.apply(applicationRequestContext).toInputValues(),
        requestId);
  }

  public KrystalExecutor getKrystalExecutor() {
    return krystalExecutor;
  }

  @Override
  public void flush() {
    krystalExecutor.flush();
  }

  @Override
  public void close() {
    krystalExecutor.close();
  }
}
