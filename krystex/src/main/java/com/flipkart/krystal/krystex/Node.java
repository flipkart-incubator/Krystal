package com.flipkart.krystal.krystex;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.ToString;

@ToString(of = {"nodeId", "executionTriggered"})
public final class Node<T> {

  private final NodeDefinition<T> nodeDefinition;
  private final NodeRegistry nodeRegistry;
  private final Result<T> result = new Result<>(new CompletableFuture<>());
  private final String nodeId;
  private final List<LogicDecorationStrategy> decorationStrategies;
  private boolean executionTriggered;

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

  public Result<T> getResult() {
    return result;
  }

  void execute() {
    if (nodeRegistry.getAll(nodeDefinition.inputs()).values().stream()
        .anyMatch(node -> !node.isDone())) {
      throw new IllegalStateException();
    }
    ImmutableMap<String, Result<?>> dependencyResults =
        nodeDefinition.inputs().stream()
            .collect(toImmutableMap(identity(), s -> nodeRegistry.get(s).getResult()));
    CompletableFuture<T> resultFuture = result.future();
    // The following implementation treats all dependencies as mandatory
    // TODO add support for optional dependencies.
    if (dependencyResults.values().stream().anyMatch(Result::isFailure)) {
      Map<String, Throwable> reasons =
          dependencyResults.entrySet().stream()
              .filter(e -> e.getValue().isFailure())
              .collect(
                  Collectors.toMap(
                      Entry::getKey,
                      e -> e.getValue().future().handle((o, throwable) -> throwable).getNow(null)));
      resultFuture.completeExceptionally(new MandatoryDependencyFailureException(reasons));
    } else {
      decoratedLogic()
          .apply(
              dependencyResults.entrySet().stream()
                  .collect(toImmutableMap(Entry::getKey, e -> e.getValue().future().getNow(null))))
          .whenComplete(
              (t, throwable) -> {
                if (throwable != null) {
                  resultFuture.completeExceptionally(throwable);
                } else {
                  resultFuture.complete(t);
                }
              });
    }
    executionTriggered = true;
  }

  private Function<ImmutableMap<String, ?>, CompletableFuture<T>> decoratedLogic() {
    Function<ImmutableMap<String, ?>, CompletableFuture<T>> logic = nodeDefinition::logic;
    for (LogicDecorationStrategy decorationStrategy : decorationStrategies) {
      logic = decorationStrategy.decorateLogic(this, logic);
    }
    return logic;
  }

  boolean wasExecutionTriggered() {
    return executionTriggered;
  }

  boolean isDone() {
    return result.future().isDone();
  }

  public NodeDefinition<T> definition() {
    return nodeDefinition;
  }

  public String getNodeId() {
    return nodeId;
  }
}
