package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutor;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutorConfig;
import com.flipkart.krystal.krystex.node.NodeExecutionConfig;
import com.flipkart.krystal.utils.MultiLeasePool;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.exec.VajramExecutor;
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
      C applicationRequestContext,
      MultiLeasePool<? extends ExecutorService> executorServicePool,
      KrystalNodeExecutorConfig config) {
    this.vajramNodeGraph = vajramNodeGraph;
    this.applicationRequestContext = applicationRequestContext;
    this.krystalExecutor =
        new KrystalNodeExecutor(
            vajramNodeGraph.getNodeDefinitionRegistry(),
            executorServicePool,
            config,
            applicationRequestContext.requestId());
  }

  @Override
  public <T> CompletableFuture<T> execute(
      VajramID vajramId, Function<C, VajramRequest> vajramRequestBuilder) {
    return execute(
        vajramId,
        vajramRequestBuilder,
        NodeExecutionConfig.builder().executionId("defaultExecution").build());
  }

  public <T> CompletableFuture<T> execute(
      VajramID vajramId,
      Function<C, VajramRequest> vajramRequestBuilder,
      NodeExecutionConfig executionConfig) {
    return krystalExecutor.executeNode(
        vajramNodeGraph.getNodeId(vajramId),
        vajramRequestBuilder.apply(applicationRequestContext).toInputValues(),
        executionConfig);
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
