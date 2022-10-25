package com.flipkart.krystal.krystex;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    this.mainLoopTask = newSingleThreadExecutor().submit(this::mainLoop);
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
        if (currentNode.hasExecuted()) {
          // TODO Emit a no-op metric that shows that a node was added to the task queue
          // unnecessarily
          continue;
        }
        NodeDefinition<?> nodeDefinition = currentNode.definition();
        Set<Node<?>> inputs = nodeRegistry.getAll(nodeDefinition.inputs());
        boolean hasPendingInputs = executePendingInputs(currentNode, inputs);
        if (!hasPendingInputs) {
          CompletableFuture<?> future = currentNode.execute();
          future.whenComplete(
              (o, throwable) ->
                  taskQueue.addAll(nodeRegistry.getAll(currentNode.definition().dependants())));
        }
      } catch (InterruptedException ignored) {

      }
    }
  }

  private boolean executePendingInputs(Node<?> currentNode, Set<Node<?>> inputNodes) {
    boolean hasPendingInputs = false;
    List<CompletableFuture<?>> inputFutures = new ArrayList<>();
    for (Node<?> inputNode : inputNodes) {
      if (!inputNode.hasExecuted()) {
        hasPendingInputs = true;
        enqueue(inputNode);
        inputFutures.add(inputNode.getResult().future());
      }
    }
    allOf(inputFutures.toArray(new CompletableFuture[] {}))
        .thenAccept((o) -> taskQueue.add(currentNode));
    return hasPendingInputs;
  }
}
