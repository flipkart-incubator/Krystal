package com.flipkart.krystal.krystex;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;

/** Default implementation of Krystal executor which */
public class KrystalTaskExecutor implements KrystalExecutor {

  private boolean shutdownRequested;
  private final NodeRegistry nodeRegistry;
  private final BlockingQueue<Node<?>> taskQueue = new LinkedBlockingDeque<>();
  private final ExecutorService singleThreadExecutor = newSingleThreadExecutor();

  public KrystalTaskExecutor(NodeRegistry nodeRegistry) {
    this.nodeRegistry = nodeRegistry;
    this.singleThreadExecutor.submit(this::mainLoop);
  }

  public void shutdown() {
    this.shutdownRequested = true;
  }

  @Override
  public <T> CompletableFuture<Result<T>> addNode(Node<T> node) {
    if (!shutdownRequested) {
      throw new IllegalStateException("Krystal has already been shutdown.");
    }
    taskQueue.add(node);
    return node.getOutput();
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

  private boolean executePendingInputs(Node<?> currentNode, Set<Node<?>> inputs) {
    boolean hasPendingInputs = false;
    List<CompletableFuture<?>> inputFutures = new ArrayList<>();
    for (Node<?> input : inputs) {
      if (!input.hasExecuted()) {
        hasPendingInputs = true;
        inputFutures.add(addNode(input));
      }
    }
    allOf(inputFutures.toArray(new CompletableFuture[] {}))
        .thenAccept((o) -> taskQueue.add(currentNode));
    return hasPendingInputs;
  }
}
