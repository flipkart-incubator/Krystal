package com.flipkart.krystal.krystex;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;

/** Default implementation of Krystal executor which */
public final class KrystalTaskExecutor implements KrystalExecutor {

  private final NodeRegistry nodeRegistry;
  private final BlockingQueue<Node<?>> taskQueue = new LinkedBlockingDeque<>();
  private final Future<?> mainLoopTask;

  private boolean shutdownRequested;

  public KrystalTaskExecutor(NodeDefinitionRegistry nodeDefinitionRegistry) {
    this.nodeRegistry = new NodeRegistry(nodeDefinitionRegistry);
    this.mainLoopTask =
        newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("KrystalTaskExecutorMonoThread").build())
            .submit(this::mainLoop);
  }

  public synchronized void shutdown() {
    this.shutdownRequested = true;
    this.mainLoopTask.cancel(true);
  }

  @Override
  public <T> Result<T> requestExecution(NodeDefinition<T> nodeDefinition) {
    if (shutdownRequested) {
      throw new IllegalStateException("Krystal has already been shutdown.");
    }
    // TODO Implement caching
    Node<T> node = new Node<>(nodeDefinition, nodeRegistry, nodeDefinition.nodeId(), emptyList());
    this.nodeRegistry.add(node);
    enqueue(node);
    return node.getResult();
  }

  private void enqueue(Node<?> node) {
    taskQueue.add(node);
  }

  private void mainLoop() {
    while (!shutdownRequested) {
      try {
        Node<?> currentNode = taskQueue.take();
        if (currentNode.wasExecutionTriggered()) {
          // TODO Emit a no-op metric that shows that a node was added to the task queue
          // unnecessarily
          continue;
        }
        boolean hasPendingInputs = enqueuePendingInputs(currentNode);
        if (!hasPendingInputs) {
          currentNode.execute();
        }
      } catch (InterruptedException ignored) {

      }
    }
  }

  private boolean enqueuePendingInputs(Node<?> currentNode) {
    NodeDefinition<?> nodeDefinition = currentNode.definition();
    boolean hasPendingInputs = false;
    List<CompletableFuture<?>> inputFutures = new ArrayList<>();
    for (String depNodeId : nodeDefinition.inputs()) {
      Node<Object> depNode = nodeRegistry.get(depNodeId);
      if (depNode == null) {
        hasPendingInputs = true;
        inputFutures.add(
            requestExecution(nodeRegistry.getNodeDefinitionRegistry().get(depNodeId)).future());
      } else {
        if (!depNode.isDone()) {
          hasPendingInputs = true;
          if (!depNode.wasExecutionTriggered()) {
            enqueue(depNode);
          }
        }
        inputFutures.add(depNode.getResult().future());
      }
    }
    // This implementation treats all dependencies as mandatory
    // TODO Add support for optional dependencies
    // TODO Add support for deadlines/timeouts
    // TODO Add support for deadline propagation
    if (hasPendingInputs) {
      allOf(inputFutures.toArray(new CompletableFuture[] {}))
          .whenComplete((o, t) -> enqueue(currentNode));
    }
    return hasPendingInputs;
  }
}
