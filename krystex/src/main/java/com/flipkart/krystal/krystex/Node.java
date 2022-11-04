package com.flipkart.krystal.krystex;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.ToString;

@ToString(of = {"nodeId", "executionTriggered"})
public final class Node<T> {

  private final NodeDefinition<T> nodeDefinition;
  private final NodeRegistry nodeRegistry;
  private final Map<Request, ImmutableList<Result<T>>> results = new HashMap<>();
  private final CompletableFuture<Void> completion = new CompletableFuture<>();
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

  public Map<Request, List<Result<T>>> getResults() {
    return ImmutableMap.copyOf(results);
  }

  void execute() {
    if (nodeRegistry.getAll(nodeDefinition.inputs().values()).values().stream()
        .anyMatch(node -> !node.isDone())) {
      throw new IllegalStateException();
    }
    ImmutableMap<String, ImmutableList<Result<?>>> dependencyResults =
        nodeDefinition.inputs().values().stream()
            .collect(
                toImmutableMap(
                    identity(),
                    s ->
                        nodeRegistry.get(s).getResults().values().stream()
                            .flatMap(Collection::stream)
                            .collect(toImmutableList())));

    // This is the list of batched responses from each dependency. If dep1 returns batch of size
    // m, dep2 returns batch of size n and dep3 returns batch of size p, the logic of this
    // vajram needs to be called m X n X p times.
    ImmutableList<Request> requests =
        createIndividualRequestsFromBatchResponses(dependencyResults).stream()
            .map(Request::new)
            .collect(toImmutableList());

    // The following implementation treats all dependencies as mandatory
    // TODO add support for optional dependencies.
    for (Request request : requests) {
      List<Result<T>> resultsForRequest = new ArrayList<>();
      if (request.asMap().values().stream().anyMatch(Result::isFailure)) {
        ImmutableMap<String, Throwable> reasons =
            request.asMap().entrySet().stream()
                .filter(e -> e.getValue().isFailure())
                .collect(
                    toImmutableMap(
                        Entry::getKey,
                        e ->
                            e.getValue()
                                .future()
                                .handle((o, throwable) -> throwable)
                                .getNow(null)));

        resultsForRequest.add(
            new Result<>(
                CompletableFuture.failedFuture(new MandatoryDependencyFailureException(reasons))));
      } else {
        decoratedLogic()
            .apply(request.asMap())
            .whenComplete(
                (t, throwable) -> {
                  if (throwable != null) {
                    resultsForRequest.add(new Result<>(CompletableFuture.failedFuture(throwable)));
                  } else {
                    resultsForRequest.addAll(
                        t.stream()
                            .map(
                                response ->
                                    new Result<>(CompletableFuture.completedFuture(response)))
                            .collect(toImmutableList()));
                  }
                });
      }
      CompletableFuture.allOf(
              results.values().stream()
                  .flatMap(Collection::stream)
                  .map(Result::future)
                  .toArray(CompletableFuture[]::new))
          .whenComplete(
              (unused, throwable) -> {
                if (throwable == null) {
                  completion.complete(null);
                } else {
                  completion.completeExceptionally(throwable);
                }
              });
      results.put(request, ImmutableList.copyOf(resultsForRequest));
    }
    executionTriggered = true;
  }

  /**
   * If input is
   *
   * <pre>
   *   {
   *     k1: {k1v1,k1v2}
   *     k2: {k2v1,k2v2}
   *   }
   * </pre>
   *
   * Then output will be
   *
   * <pre>
   *   [
   *     {
   *       k1:k1v1
   *       k2:k2v1
   *     },
   *     {
   *       k1:k1v1
   *       k2:k2v2
   *     },
   *     {
   *       k1:k1v2
   *       k2:k2v1
   *     },
   *     {
   *       k1:k1v2
   *       k2:k2v2
   *     },
   *   ]
   * </pre>
   */
  private ImmutableList<ImmutableMap<String, Result<?>>> createIndividualRequestsFromBatchResponses(
      Map<String, ImmutableList<Result<?>>> batchResults) {
    if (batchResults.isEmpty()) {
      return ImmutableList.of();
    }
    String first = batchResults.keySet().iterator().next();
    ImmutableList<ImmutableMap<String, Result<?>>> individualRequestsFromBatchResponses =
        createIndividualRequestsFromBatchResponses(
            // Create a subMapView of all keys except the first one.
            Maps.filterKeys(
                batchResults, key -> !Objects.equals(first, key) && batchResults.containsKey(key)));
    ImmutableList.Builder<ImmutableMap<String, Result<?>>> answer = ImmutableList.builder();
    for (ImmutableMap<String, Result<?>> subMap : individualRequestsFromBatchResponses) {
      Builder<String, Result<?>> builder = ImmutableMap.builder();
      for (Result<?> result : batchResults.get(first)) {
        answer.add(builder.putAll(subMap).put(first, result).build());
      }
    }
    return answer.build();
  }

  private Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> decoratedLogic() {
    Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> logic =
        nodeDefinition::logic;
    for (LogicDecorationStrategy decorationStrategy : decorationStrategies) {
      logic = decorationStrategy.decorateLogic(this, logic);
    }
    return logic;
  }

  boolean wasExecutionTriggered() {
    return executionTriggered;
  }

  boolean isDone() {
    return completion.isDone();
  }

  public NodeDefinition<T> definition() {
    return nodeDefinition;
  }

  public String getNodeId() {
    return nodeId;
  }
}
