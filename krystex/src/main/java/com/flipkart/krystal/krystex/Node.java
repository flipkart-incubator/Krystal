package com.flipkart.krystal.krystex;

import static com.flipkart.krystal.krystex.NodeState.DEPENDENCIES_INITIATED;
import static com.flipkart.krystal.krystex.NodeState.DONE;
import static com.flipkart.krystal.krystex.NodeState.INITIATED;
import static com.flipkart.krystal.krystex.NodeState.NEW;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Sets.union;
import static java.util.Arrays.stream;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.failedFuture;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString(of = {"nodeId", "nodeState"})
@Slf4j
public final class Node<T> {

  private final NodeDefinition<T> nodeDefinition;
  private final String nodeId;
  private final List<LogicDecorationStrategy> decorationStrategies;

  private final AtomicReference<NodeState> nodeState = new AtomicReference<>(NEW);

  private final List<Consumer<ImmutableCollection<SingleResult<T>>>> newDataSubscriptions =
      new ArrayList<>();
  /** Remembers whether newDataSubscriptions were notified about results of a request */
  private final Map<Request, CompletableFuture<?>> dataTransferDone = new LinkedHashMap<>();

  private final List<Runnable> doneSubscriptions = new ArrayList<>();
  private final Map<String, Boolean> inputsDone = new HashMap<>();

  private final Map<String, Collection<SingleResult<?>>> resultsForInput = new HashMap<>();
  private final Map<Request, BatchResult<T>> resultsByRequest = new HashMap<>();
  private final CompletableFuture<ImmutableList<T>> allResults = new CompletableFuture<>();

  public static <T> Node<T> createNode(
      NodeDefinition<T> nodeDefinition, List<LogicDecorationStrategy> decorationStrategies) {
    return new Node<>(nodeDefinition, decorationStrategies);
  }

  private Node(
      NodeDefinition<T> nodeDefinition, List<LogicDecorationStrategy> decorationStrategies) {
    this.nodeDefinition = nodeDefinition;
    this.decorationStrategies = decorationStrategies;
    this.nodeId = nodeDefinition.nodeId();
  }

  void markDependencyNodeDone(String nodeId) {
    getInputsProvidedByNode(nodeId).forEach(this::markInputDone);
  }

  void markInputDone(String input) {
    inputsDone.put(input, true);
    boolean allDependenciesDone =
        nodeDefinition.inputNames().stream().allMatch(key -> inputsDone.getOrDefault(key, false));
    if (allDependenciesDone) {
      markDone();
    }
  }

  public void executeIfNoDependenciesAndMarkDone() {
    if (!nodeDefinition.inputNames().isEmpty()) {
      throw new IllegalStateException("This node has input");
    }
    execute(ImmutableList.of(new Request()));
    markDone();
  }

  void executeWithNewDataForDependencyNode(String depNodeId, Collection<SingleResult<?>> newData) {
    Set<String> inputsProvidedByNode = getInputsProvidedByNode(depNodeId);
    inputsProvidedByNode.forEach(input -> executeWithNewDataForInput(input, newData));
  }

  private Set<String> getInputsProvidedByNode(String depNodeId) {
    return nodeDefinition.inputProviders().entrySet().stream()
        .filter(entry -> Objects.equals(entry.getValue(), depNodeId))
        .map(Entry::getKey)
        .collect(Collectors.toSet());
  }

  /**
   * Re-execute this node as more data was made available by a input (For example because if it made
   * two batched calls and the second call took longer).
   *
   * @param input The input which has made new data available
   * @param newData the new data made available by the input
   * @return the new results computed by this node in light of the new data presented to this
   *     method.
   */
  void executeWithNewDataForInput(String input, Collection<SingleResult<?>> newData) {
    if (!newData.stream().map(SingleResult::future).allMatch(CompletableFuture::isDone)) {
      throw new IllegalArgumentException(
          "executeWithNewDataForInput can only be called after the data is ready");
    }
    if (!INITIATED.equals(nodeState.get())
        && !nodeState.compareAndSet(DEPENDENCIES_INITIATED, INITIATED)) {
      throw new IllegalStateException(
          "Only DEPENDENCIES_INITIATED and INITIATED nodes can be executed");
    }

    getResultsForInput(input).addAll(newData);
    // Check if there is data available for all other dependencies of this node except the one
    // passed to this method.
    if (!union(Set.of(input), resultsForInput.keySet()).equals(nodeDefinition.inputNames())) {
      // Since some dependencies' data is still not received, we cannot execute this node yet.
      return;
    }
    // Get data for all other dependencies...
    Map<String, Collection<SingleResult<?>>> newDataCombinations =
        new LinkedHashMap<>(Maps.filterKeys(resultsForInput, k -> !Objects.equals(k, input)));
    // ...and add this new data so that all new permutations of requests are executed.
    newDataCombinations.put(input, ImmutableList.copyOf(newData));
    ImmutableList<Request> requests =
        createIndividualRequestsFromBatchResponses(newDataCombinations).stream()
            .map(Request::new)
            .collect(toImmutableList());
    execute(requests);
  }

  private ImmutableMap<Request, BatchResult<T>> execute(ImmutableList<Request> requests) {
    // The following implementation treats all dependencies as mandatory
    // TODO add support for optional dependencies.
    Map<Request, BatchResult<T>> newResults = new LinkedHashMap<>();
    for (Request request : requests) {
      BatchResult<T> resultsForRequest;
      if (request.asMap().values().stream().anyMatch(SingleResult::isFailure)) {
        ImmutableMap<String, Throwable> reasons =
            request.asMap().entrySet().stream()
                .filter(e -> e.getValue().isFailure())
                .collect(
                    toImmutableMap(
                        Entry::getKey,
                        e ->
                            e.getValue()
                                .future()
                                .handle((o, throwable1) -> throwable1)
                                .getNow(null)));

        resultsForRequest =
            new BatchResult<>(failedFuture(new MandatoryDependencyFailureException(reasons)));
      } else {
        resultsForRequest =
            new BatchResult<>(decoratedLogic().apply(getValuesForConsumption(request)));
      }
      newResults.put(request, resultsForRequest);
    }
    // Notify dependants that new data is available from this node
    newResults.forEach(
        (request, batchResult) -> {
          dataTransferDone.put(
              request,
              batchResult
                  .future()
                  .handle(
                      (ts, throwable) -> {
                        synchronized (newDataSubscriptions) {
                          try {
                            newDataSubscriptions.forEach(
                                c -> c.accept(batchResult.toSingleResults()));
                          } catch (Exception e) {
                            log.warn("Exception when notifying new Data availability", e);
                          }
                        }
                        return null;
                      }));
        });
    resultsByRequest.putAll(newResults);
    return ImmutableMap.copyOf(newResults);
  }

  // TODO Implement scenario where a mandatory input of a node is an error
  private ImmutableMap<String, Object> getValuesForConsumption(Request request) {
    ImmutableMap<String, SingleResult<?>> map = request.asMap();
    Map<String, Object> values = new HashMap<>();
    map.forEach((input, singleResult) -> values.put(input, singleResult.future().getNow(null)));
    return ImmutableMap.copyOf(values);
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
  private static ImmutableList<ImmutableMap<String, SingleResult<?>>>
      createIndividualRequestsFromBatchResponses(
          Map<String, Collection<SingleResult<?>>> batchResults) {
    if (batchResults.isEmpty()) {
      return ImmutableList.of(ImmutableMap.of());
    }
    String first = batchResults.keySet().iterator().next();
    ImmutableList<ImmutableMap<String, SingleResult<?>>> individualRequestsFromBatchResponses =
        createIndividualRequestsFromBatchResponses(
            // Create a subMapView of all keys except the first one.
            Maps.filterKeys(
                batchResults, key -> !Objects.equals(first, key) && batchResults.containsKey(key)));
    ImmutableList.Builder<ImmutableMap<String, SingleResult<?>>> answer = ImmutableList.builder();
    for (ImmutableMap<String, SingleResult<?>> subMap : individualRequestsFromBatchResponses) {
      Builder<String, SingleResult<?>> builder = ImmutableMap.builder();
      for (SingleResult<?> result : batchResults.get(first)) {
        answer.add(builder.putAll(subMap).put(first, result).build());
      }
    }
    return answer.build();
  }

  private void markDone() {
    ImmutableList<BatchResult<T>> listOfBatches =
        resultsByRequest.values().stream().collect(toImmutableList());
    allOf(listOfBatches.stream().map(BatchResult::future).toArray(CompletableFuture[]::new))
        .thenCompose(
            void1 -> {
              //noinspection unchecked
              CompletableFuture<T>[] cfs =
                  listOfBatches.stream()
                      .map(BatchResult::toSingleResults)
                      .flatMap(Collection::stream)
                      .map(SingleResult::future)
                      .toArray(CompletableFuture[]::new);
              return allOf(cfs)
                  .thenApply(
                      void2 -> stream(cfs).map(f -> f.getNow(null)).collect(toImmutableList()));
            })
        .whenComplete(
            (ts, throwable) -> {
              if (throwable != null) {
                allResults.completeExceptionally(throwable);
              } else {
                allResults.complete(ts);
              }
              nodeState.set(DONE);
              synchronized (doneSubscriptions) {
                for (Runnable runnable : doneSubscriptions) {
                  try {
                    runnable.run();
                  } catch (Exception e) {
                    log.error("Error while notifying done subscriptions", e);
                  }
                }
              }
            });
  }

  private Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> decoratedLogic() {
    Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> logic =
        nodeDefinition::logic;
    for (LogicDecorationStrategy decorationStrategy : decorationStrategies) {
      logic = decorationStrategy.decorateLogic(this, logic);
    }
    return logic;
  }

  boolean wasInitiated() {
    return this.nodeState.get().ordinal() >= INITIATED.ordinal();
  }

  public NodeDefinition<T> definition() {
    return nodeDefinition;
  }

  public String getNodeId() {
    return this.nodeId;
  }

  public void whenNewDataAvailable(Consumer<ImmutableCollection<SingleResult<T>>> newDataConsumer) {
    synchronized (newDataSubscriptions) {
      ImmutableSet<Request> newDataNotifiedRequests =
          this.dataTransferDone.entrySet().stream()
              .filter(e -> e.getValue().isDone())
              .map(Entry::getKey)
              .collect(toImmutableSet());
      newDataConsumer.accept(
          resultsByRequest.entrySet().stream()
              .filter(e -> newDataNotifiedRequests.contains(e.getKey()))
              .map(Entry::getValue)
              .filter(tBatchResult -> tBatchResult.future().isDone())
              .map(BatchResult::toSingleResults)
              .flatMap(Collection::stream)
              .collect(toImmutableList()));
      this.newDataSubscriptions.add(newDataConsumer);
    }
  }

  public void whenDone(Runnable onDone) {
    synchronized (doneSubscriptions) {
      if (DONE.equals(nodeState.get())) {
        onDone.run();
      } else {
        this.doneSubscriptions.add(onDone);
      }
    }
  }

  public void markDependenciesInitiated() {
    if (!this.nodeState.compareAndSet(NEW, DEPENDENCIES_INITIATED)) {
      throw new IllegalStateException(
          "DEPENDENCIES_INITIATED state should follow NEW state, not %s"
              .formatted(nodeState.get()));
    }
  }

  public boolean wereDependenciesInitiated() {
    return this.nodeState.get().ordinal() >= DEPENDENCIES_INITIATED.ordinal();
  }

  public CompletableFuture<ImmutableList<T>> getAllResults() {
    return allResults;
  }

  private Collection<SingleResult<?>> getResultsForInput(String input) {
    return resultsForInput.computeIfAbsent(input, k -> new ArrayList<>());
  }
}
