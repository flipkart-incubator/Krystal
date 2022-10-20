package com.flipkart.krystal.krystex;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class Node<T> {

  private final NodeDefinition<T> nodeDefinition;
  private final NodeRegistry nodeRegistry;
  private final CompletableFuture<T> future = new CompletableFuture<>();
  private final String nodeId;
  private final List<LogicDecorationStrategy> decorationStrategies;

  private boolean hasExecuted;

  public Node(
      NodeDefinition<T> nodeDefinition,
      NodeRegistry nodeRegistry,
      String nodeId,
      List<LogicDecorationStrategy> decorationStrategies) {
    this.nodeDefinition = nodeDefinition;
    this.nodeRegistry = nodeRegistry;
    this.nodeId = nodeId;
    this.decorationStrategies = decorationStrategies;
  }

  abstract CompletableFuture<Result<T>> getOutput();

  final CompletableFuture<T> execute() {
    if (nodeRegistry.getAll(nodeDefinition.inputs()).stream()
        .anyMatch(node -> !node.hasExecuted())) {
      throw new IllegalStateException();
    }
    try {
      decoratedLogic()
          .whenComplete(
              (t, throwable) -> {
                if (throwable != null) {
                  future.completeExceptionally(throwable);
                } else {
                  future.complete(t);
                }
              });
    } catch (Exception e) {
      future.completeExceptionally(e);
    } finally {
      hasExecuted = true;
    }
    return future;
  }

  private CompletionStage<T> decoratedLogic() {
    CompletionStage<T> result = nodeDefinition.logic();
    for (LogicDecorationStrategy decorationStrategy : decorationStrategies) {
      result = decorationStrategy.decorateLogic(this).get();
    }
    return result;
  }

  boolean hasExecuted() {
    return hasExecuted;
  }

  public NodeDefinition<T> definition() {
    return nodeDefinition;
  }

  public String getNodeId() {
    return nodeId;
  }
}
