package com.flipkart.krystal.krystex.node;

import static com.flipkart.krystal.data.ValueOrError.empty;
import static com.flipkart.krystal.data.ValueOrError.withError;
import static com.flipkart.krystal.utils.Futures.linkFutures;
import static com.google.common.base.Functions.identity;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.lang.Math.max;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.commands.BatchCommand;
import com.flipkart.krystal.krystex.commands.DependencyCallbackBatch;
import com.flipkart.krystal.krystex.commands.ExecuteWithDependency;
import com.flipkart.krystal.krystex.commands.ExecuteWithInputs;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.NodeInputBatch;
import com.flipkart.krystal.krystex.commands.NodeInputCommand;
import com.flipkart.krystal.krystex.commands.NodeRequestCommand;
import com.flipkart.krystal.krystex.commands.SkipNode;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutor.DependencyExecStrategy;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.krystex.resolution.DependencyResolutionRequest;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverCommand;
import com.flipkart.krystal.krystex.resolution.ResolverCommand.SkipDependency;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.flipkart.krystal.utils.ImmutableMapView;
import com.flipkart.krystal.utils.SkippedExecutionException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings({"ClassWithTooManyFields", "OverlyComplexClass"})
class Node {

  private static final long TIMEOUT_MS = 1000000;

  private final NodeId nodeId;

  private final NodeDefinition nodeDefinition;

  private final KrystalNodeExecutor krystalNodeExecutor;

  /** decoratorType -> Decorator */
  private final Function<LogicExecutionContext, ImmutableMap<String, MainLogicDecorator>>
      requestScopedDecoratorsSupplier;

  private final ImmutableMapView<Optional<String>, List<ResolverDefinition>>
      resolverDefinitionsByInput;
  private final ImmutableMapView<String, ImmutableSet<ResolverDefinition>>
      resolverDefinitionsByDependencies;
  private final LogicDecorationOrdering logicDecorationOrdering;

  private final Map<RequestId, Map<String, DependencyNodeExecutions>> dependencyExecutions =
      new LinkedHashMap<>();

  private final Map<RequestId, Map<String, InputValue<Object>>> inputsValueCollector =
      new LinkedHashMap<>();

  private final Map<RequestId, Map<String, Results<Object>>> dependencyValuesCollector =
      new LinkedHashMap<>();

  private final Map<DependantChain, Set<String>> collectedInputNames = new LinkedHashMap<>();

  /** A unique Result future for every requestId. */
  private final Map<RequestId, CompletableFuture<NodeResponse>> resultsByRequest =
      new LinkedHashMap<>();
  /** A unique Result future for every requestId. */
  private final Map<DependantChain, CompletableFuture<NodeBatchResponse>> resultsByBatch =
      new LinkedHashMap<>();

  /**
   * A unique {@link CompletableFuture} for every new set of Inputs. This acts as a cache so that
   * the same computation is not repeated multiple times .
   */
  private final Map<Inputs, CompletableFuture<Object>> resultsCache = new LinkedHashMap<>();

  private final Map<RequestId, Boolean> mainLogicExecuted = new LinkedHashMap<>();

  private final Map<RequestId, Optional<SkipNode>> skipLogicRequested = new LinkedHashMap<>();

  private final Map<RequestId, Map<ResolverDefinition, ResolverCommand>> resolverResults =
      new LinkedHashMap<>();

  private final Map<DependantChain, Boolean> flushedDependantChain = new LinkedHashMap<>();
  private final Map<DependantChain, Set<RequestId>> requestsByDependantChain =
      new LinkedHashMap<>();
  private final Map<RequestId, DependantChain> dependantChainByRequest = new LinkedHashMap<>();
  private final DependencyExecStrategy dependencyExecStrategy;
  private final NodeMetrics nodeMetrics;

  Node(
      NodeDefinition nodeDefinition,
      KrystalNodeExecutor krystalNodeExecutor,
      Function<LogicExecutionContext, ImmutableMap<String, MainLogicDecorator>>
          requestScopedDecoratorsSupplier,
      LogicDecorationOrdering logicDecorationOrdering,
      DependencyExecStrategy dependencyExecStrategy,
      NodeMetrics nodeMetrics) {
    this.dependencyExecStrategy = dependencyExecStrategy;
    this.nodeId = nodeDefinition.nodeId();
    this.nodeDefinition = nodeDefinition;
    this.krystalNodeExecutor = krystalNodeExecutor;
    this.requestScopedDecoratorsSupplier = requestScopedDecoratorsSupplier;
    this.logicDecorationOrdering = logicDecorationOrdering;
    this.resolverDefinitionsByInput =
        createResolverDefinitionsByInputs(nodeDefinition.resolverDefinitions());
    this.resolverDefinitionsByDependencies =
        ImmutableMapView.viewOf(
            nodeDefinition.resolverDefinitions().stream()
                .collect(groupingBy(ResolverDefinition::dependencyName, toImmutableSet())));
    this.nodeMetrics = nodeMetrics;
  }

  void executeCommand(Flush nodeCommand) {
    flushedDependantChain.put(nodeCommand.nodeDependants(), true);
    flushAllDependenciesIfNeeded(nodeCommand.nodeDependants());
    flushDecoratorsIfNeeded(nodeCommand.nodeDependants());
  }

  CompletableFuture<NodeResponse> executeRequestCommand(NodeRequestCommand nodeCommand) {
    return measuringTimeTaken(
        () -> {
          RequestId requestId = nodeCommand.requestId();
          final CompletableFuture<NodeResponse> resultForRequest =
              resultsByRequest.computeIfAbsent(requestId, r -> new CompletableFuture<>());
          if (resultForRequest.isDone()) {
            // This is possible if this node was already skipped, for example.
            // If the result for this requestId is already available, just return and avoid
            // unnecessary
            // computation.
            return resultForRequest;
          }
          try {
            if (nodeCommand instanceof SkipNode skipNode) {
              resultForRequest.completeExceptionally(skipNodeException(skipNode));
            }
            List<NodeInputCommand> nodeRequestCommands = computeNodeCommands(nodeCommand);
            propagateCommands(requestId, nodeRequestCommands);
            if (nodeRequestCommands.isEmpty()) {
              executeMainLogicIfPossible(requestId)
                  .ifPresent(mainLogicResult -> linkFutures(mainLogicResult, resultForRequest));
            }
          } catch (Throwable e) {
            resultForRequest.completeExceptionally(e);
          }
          return resultForRequest;
        },
        timeTaken -> nodeMetrics.totalNodeTimeNs(timeTaken.toNanos()));
  }

  CompletableFuture<NodeBatchResponse> executeBatchCommand(BatchCommand<?> nodeInputBatchCommand) {
    return measuringTimeTaken(
        () -> {
          if (nodeInputBatchCommand instanceof DependencyCallbackBatch) {
            nodeMetrics.depCallbackBatchCount();
          } else if (nodeInputBatchCommand instanceof NodeInputBatch) {
            nodeMetrics.nodeInputsBatchCount();
          }
          Map<RequestId, ? extends NodeRequestCommand> subCommands =
              nodeInputBatchCommand.subCommands();
          CompletableFuture<NodeBatchResponse> batchFuture =
              resultsByBatch.computeIfAbsent(
                  nodeInputBatchCommand.dependantChain(), requestId -> new CompletableFuture<>());
          if (batchFuture.isDone()) {
            // This is possible if this node was already skipped, for example.
            // If the result for this requestId is already available, just return and avoid
            // unnecessary
            // computation.
            return batchFuture;
          }
          try {

            Map<String, Map<RequestId, List<NodeInputCommand>>> outgoingCommandsByDep =
                computeNodeCommands(nodeInputBatchCommand);
            propagateCommands(nodeInputBatchCommand, outgoingCommandsByDep);
            Map<RequestId, NodeResponse> skipResults = new LinkedHashMap<>();
            subCommands.values().stream()
                .map(
                    c ->
                        skipLogicRequested
                            .getOrDefault(c.requestId(), Optional.empty())
                            .orElse(null))
                .filter(Objects::nonNull)
                .forEach(
                    skipNode -> {
                      skipResults.put(
                          skipNode.requestId(),
                          new NodeResponse(
                              Inputs.empty(),
                              withError(skipNodeException(skipNode)),
                              skipNode.requestId()));
                    });

            Optional<CompletableFuture<Map<RequestId, NodeResponse>>> mainLogicFuture =
                executeMainLogicIfPossible(
                    subCommands.values().stream()
                        .map(NodeRequestCommand::requestId)
                        .filter(
                            key -> skipLogicRequested.getOrDefault(key, Optional.empty()).isEmpty())
                        .toList(),
                    nodeInputBatchCommand.dependantChain());
            if (mainLogicFuture.isPresent()) {
              linkFutures(
                  mainLogicFuture
                      .get()
                      .thenApply(
                          map -> {
                            map.putAll(skipResults);
                            return map;
                          })
                      .thenApply(NodeBatchResponse::new),
                  batchFuture);
            } else if (skipResults.size() == subCommands.size()) {
              batchFuture.complete(new NodeBatchResponse(skipResults));
            }
          } catch (Throwable e) {
            batchFuture.completeExceptionally(e);
          }
          return batchFuture;
        },
        timeTaken -> nodeMetrics.totalNodeTimeNs(timeTaken.toNanos()));
  }

  private void propagateCommands(RequestId requestId, List<NodeInputCommand> nodeRequestCommands) {
    measuringTimeTaken(
        () -> {
          Set<String> dependencies = new LinkedHashSet<>();
          for (NodeRequestCommand nodeRequestCommand : nodeRequestCommands) {
            RequestId depRequestId = nodeRequestCommand.requestId();
            String dependencyName = getDependencyName(nodeRequestCommand);
            DependencyNodeExecutions dependencyNodeExecutions =
                dependencyExecutions
                    .computeIfAbsent(requestId, _r -> new LinkedHashMap<>())
                    .computeIfAbsent(dependencyName, _d -> new DependencyNodeExecutions());
            dependencyNodeExecutions
                .individualCallResponses()
                .putIfAbsent(depRequestId, krystalNodeExecutor.executeCommand(nodeRequestCommand));
            dependencies.add(dependencyName);
          }
          for (String dependencyName : dependencies) {
            registerDependencyCallbacks(
                requestId,
                dependencyName,
                nodeDefinition.dependencyNodes().get(dependencyName),
                dependencyExecutions
                    .computeIfAbsent(requestId, _r -> new LinkedHashMap<>())
                    .computeIfAbsent(dependencyName, _d -> new DependencyNodeExecutions()));
          }
        },
        timeTaken -> nodeMetrics.propagateNodeCommandsNs(timeTaken.toNanos()));
  }

  private void propagateCommands(
      BatchCommand<?> incomingBatch,
      Map<String, Map<RequestId, List<NodeInputCommand>>> outGoingBatchesByDep) {
    measuringTimeTaken(
        () -> {
          for (Entry<String, Map<RequestId, List<NodeInputCommand>>> entry :
              outGoingBatchesByDep.entrySet()) {
            String dependencyName = entry.getKey();
            Map<RequestId, List<NodeInputCommand>> nodeRequestCommands = entry.getValue();
            NodeId depNodeId = nodeDefinition.dependencyNodes().get(dependencyName);
            CompletableFuture<NodeBatchResponse> batchCompletionFuture =
                krystalNodeExecutor.executeBatchCommand(
                    new NodeInputBatch(
                        depNodeId,
                        nodeRequestCommands.values().stream()
                            .flatMap(Collection::stream)
                            .collect(toMap(NodeRequestCommand::requestId, identity())),
                        incomingBatch.dependantChain().extend(nodeId, dependencyName)));
            nodeRequestCommands.forEach(
                (requestId, nodeRequestCommandList) -> {
                  nodeRequestCommandList.forEach(
                      nodeRequestCommand -> {
                        RequestId depRequestId = nodeRequestCommand.requestId();
                        DependencyNodeExecutions dependencyNodeExecutions =
                            dependencyExecutions
                                .computeIfAbsent(requestId, _r -> new LinkedHashMap<>())
                                .computeIfAbsent(
                                    dependencyName, _d -> new DependencyNodeExecutions());
                        dependencyNodeExecutions
                            .individualCallResponses()
                            .putIfAbsent(
                                depRequestId,
                                batchCompletionFuture.thenApply(
                                    batch -> batch.responses().get(depRequestId)));
                      });
                });
            registerBatchDependencyCallbacks(
                nodeRequestCommands.keySet(), dependencyName, depNodeId, incomingBatch);
          }
        },
        timeTaken -> nodeMetrics.propagateNodeCommandsNs(timeTaken.toNanos()));
  }

  private List<NodeInputCommand> computeNodeCommands(NodeRequestCommand nodeCommand) {
    return measuringTimeTaken(
        () -> {
          List<NodeInputCommand> nodeInputCommands;
          RequestId requestId = nodeCommand.requestId();
          if (nodeCommand instanceof SkipNode skipNode) {
            requestsByDependantChain
                .computeIfAbsent(skipNode.dependantChain(), k -> new LinkedHashSet<>())
                .add(requestId);
            dependantChainByRequest.put(requestId, skipNode.dependantChain());
            skipLogicRequested.put(requestId, Optional.of(skipNode));
            nodeInputCommands = handleSkipDependency(skipNode);
          } else if (nodeCommand instanceof ExecuteWithDependency executeWithDependency) {
            nodeInputCommands = executeWithDependency(executeWithDependency);
          } else if (nodeCommand instanceof ExecuteWithInputs executeWithInputs) {
            requestsByDependantChain
                .computeIfAbsent(executeWithInputs.dependantChain(), k -> new LinkedHashSet<>())
                .add(requestId);
            dependantChainByRequest.computeIfAbsent(
                requestId, r -> executeWithInputs.dependantChain());
            nodeInputCommands = executeWithInputs(executeWithInputs);
          } else {
            throw new UnsupportedOperationException(
                "Unknown type of nodeCommand: %s".formatted(nodeCommand));
          }
          return nodeInputCommands;
        },
        timeTaken -> nodeMetrics.computeInputsForExecuteTimeNs(timeTaken.toNanos()));
  }

  private Map<String, Map<RequestId, List<NodeInputCommand>>> computeNodeCommands(
      BatchCommand<?> batchCommand) {
    List<ExecuteWithDependency> executeWithDependencyList = new ArrayList<>();
    List<ExecuteWithInputs> executeWithInputsList = new ArrayList<>();
    List<SkipNode> skipCommands = new ArrayList<>();

    Set<RequestId> allRequestIds = batchCommand.subCommands().keySet();
    Set<RequestId> requestsByDepChain =
        requestsByDependantChain.computeIfAbsent(
            batchCommand.dependantChain(), k -> new LinkedHashSet<>(allRequestIds.size()));

    measuringTimeTaken(
        () -> {
          batchCommand
              .subCommands()
              .forEach(
                  (requestId, nodeCommand) -> {
                    requestsByDepChain.add(requestId);
                    dependantChainByRequest.put(requestId, batchCommand.dependantChain());
                    if (nodeCommand instanceof SkipNode skipNode) {
                      skipLogicRequested.put(requestId, Optional.of(skipNode));
                      skipCommands.add(skipNode);
                    } else if (nodeCommand instanceof ExecuteWithDependency executeWithDependency) {
                      executeWithDependencyList.add(executeWithDependency);
                    } else if (nodeCommand instanceof ExecuteWithInputs executeWithInputs) {
                      executeWithInputsList.add(executeWithInputs);
                    } else {
                      throw new UnsupportedOperationException(
                          "Unknown type of nodeCommand: %s".formatted(nodeCommand));
                    }
                  });
        },
        timeTaken -> nodeMetrics.computeInputsForExecuteTimeNs(timeTaken.toNanos()));

    Map<String, Map<RequestId, List<NodeInputCommand>>> nodeInputCommands =
        new LinkedHashMap<>(handleSkipDependencies(skipCommands));
    measuringTimeTaken(
            () -> {
              collectInputValues(executeWithInputsList, batchCommand.dependantChain());
              collectDepValues(executeWithDependencyList, batchCommand.dependantChain());

              Map<RequestId, Set<String>> inputNamesByRequest = new LinkedHashMap<>();
              executeWithInputsList.forEach(
                  executeWithInputs ->
                      inputNamesByRequest
                          .computeIfAbsent(
                              executeWithInputs.requestId(), _k1 -> new LinkedHashSet<>())
                          .addAll(executeWithInputs.inputNames()));
              executeWithDependencyList.forEach(
                  executeWithDependency ->
                      inputNamesByRequest
                          .computeIfAbsent(
                              executeWithDependency.requestId(), _k1 -> new LinkedHashSet<>())
                          .add(executeWithDependency.dependencyName()));
              return inputNamesByRequest;
            },
            timeTaken -> nodeMetrics.computeInputsForExecuteTimeNs(timeTaken.toNanos()))
        .forEach(
            (requestId, newInputNames) -> {
              execute(requestId, newInputNames)
                  .forEach(
                      (depName, outgoingCommands) -> {
                        nodeInputCommands
                            .computeIfAbsent(depName, _k -> new LinkedHashMap<>())
                            .computeIfAbsent(requestId, _k -> new ArrayList<>())
                            .addAll(outgoingCommands);
                      });
            });
    return nodeInputCommands;
  }

  private Optional<CompletableFuture<Map<RequestId, NodeResponse>>> executeMainLogicIfPossible(
      List<RequestId> requestIds, DependantChain dependantChain) {
    // If all the inputs and dependency values are available, then prepare run mainLogic
    ImmutableSet<String> inputNames = nodeDefinition.getMainLogicDefinition().inputNames();
    return measuringTimeTaken(
        () -> {
          if (collectedInputNames
              .getOrDefault(dependantChain, ImmutableSet.of())
              .containsAll(inputNames)) { // All the inputs of the logic node have data present
            return Optional.of(executeMainLogic(requestIds, dependantChain));
          }
          return Optional.empty();
        },
        timeTaken -> nodeMetrics.mainLogicIfPossibleTimeNs(timeTaken.toNanos()));
  }

  private List<NodeInputCommand> handleSkipDependency(SkipNode skipNode) {
    RequestId requestId = skipNode.requestId();
    Set<ResolverDefinition> pendingResolvers =
        resolverDefinitionsByInput.values().stream()
            .flatMap(Collection::stream)
            .filter(
                resolverDefinition ->
                    !this.resolverResults
                        .computeIfAbsent(requestId, r -> new LinkedHashMap<>())
                        .containsKey(resolverDefinition))
            .collect(toSet());

    Optional<MultiResolverDefinition> multiResolverOpt =
        nodeDefinition
            .multiResolverLogicId()
            .map(
                nodeLogicId ->
                    nodeDefinition
                        .nodeDefinitionRegistry()
                        .logicDefinitionRegistry()
                        .getMultiResolver(nodeLogicId));
    return executeResolvers(requestId, pendingResolvers, multiResolverOpt).values().stream()
        .flatMap(Collection::stream)
        .toList();
  }

  private Map<String, Map<RequestId, List<NodeInputCommand>>> handleSkipDependencies(
      List<SkipNode> skipCommands) {
    Optional<MultiResolverDefinition> multiResolverOpt = getMultiResolverDef();
    Map<String, Map<RequestId, List<NodeInputCommand>>> outGoingNodeCommands =
        new LinkedHashMap<>();
    for (SkipNode skipCommand : skipCommands) {
      RequestId requestId = skipCommand.requestId();
      // Since this node is skipped, we need to get all the pending resolvers (irrespective of
      // whether their inputs are available or not) and mark them resolved.
      Set<ResolverDefinition> pendingResolvers =
          resolverDefinitionsByInput.values().stream()
              .flatMap(Collection::stream)
              .filter(
                  resolverDefinition ->
                      !this.resolverResults
                          .computeIfAbsent(requestId, r -> new LinkedHashMap<>())
                          .containsKey(resolverDefinition))
              .collect(toSet());
      executeResolvers(requestId, pendingResolvers, multiResolverOpt)
          .forEach(
              (depName, nodeInputCommands) -> {
                outGoingNodeCommands
                    .computeIfAbsent(depName, _k -> new LinkedHashMap<>())
                    .computeIfAbsent(requestId, _k -> new ArrayList<>())
                    .addAll(nodeInputCommands);
              });
    }
    return outGoingNodeCommands;
  }

  private Optional<MultiResolverDefinition> getMultiResolverDef() {
    return nodeDefinition
        .multiResolverLogicId()
        .map(
            nodeLogicId ->
                nodeDefinition
                    .nodeDefinitionRegistry()
                    .logicDefinitionRegistry()
                    .getMultiResolver(nodeLogicId));
  }

  private static SkippedExecutionException skipNodeException(SkipNode skipNode) {
    String reason = skipNode.skipDependencyCommand().reason();
    return skipNodeException(reason);
  }

  private static SkippedExecutionException skipNodeException(String reason) {
    return new SkippedExecutionException(reason);
  }

  private void flushDecoratorsIfNeeded(DependantChain dependantChain) {
    if (!flushedDependantChain.getOrDefault(dependantChain, false)) {
      return;
    }
    Set<RequestId> requestIds = requestsByDependantChain.get(dependantChain);
    int requestIdExecuted = 0;
    for (RequestId requestId : requestIds) {
      if (mainLogicExecuted.getOrDefault(requestId, false)
          || skipLogicRequested.getOrDefault(requestId, Optional.empty()).isPresent()) {
        requestIdExecuted += 1;
      }
    }
    if (requestIdExecuted == requestIds.size()) {
      Iterable<MainLogicDecorator> reverseSortedDecorators =
          getSortedDecorators(dependantChain)::descendingIterator;
      for (MainLogicDecorator decorator : reverseSortedDecorators) {
        decorator.executeCommand(new FlushCommand(dependantChain));
      }
    }
  }

  private List<NodeInputCommand> executeWithInputs(ExecuteWithInputs executeWithInputs) {
    collectInputValues(ImmutableList.of(executeWithInputs), executeWithInputs.dependantChain());
    return execute(executeWithInputs.requestId(), executeWithInputs.inputNames()).values().stream()
        .flatMap(Collection::stream)
        .toList();
  }

  private List<NodeInputCommand> executeWithDependency(
      ExecuteWithDependency executeWithDependency) {
    collectDepValues(
        ImmutableList.of(executeWithDependency),
        dependantChainByRequest.get(executeWithDependency.requestId()));
    return execute(
            executeWithDependency.requestId(),
            ImmutableSet.of(executeWithDependency.dependencyName()))
        .values()
        .stream()
        .flatMap(Collection::stream)
        .toList();
  }

  private void collectDepValues(
      List<ExecuteWithDependency> executeWithDependencyBatch, DependantChain dependantChain) {
    Set<String> allInputNames =
        collectedInputNames.computeIfAbsent(dependantChain, _k -> new LinkedHashSet<>());
    for (ExecuteWithDependency executeWithDependency : executeWithDependencyBatch) {
      RequestId requestId = executeWithDependency.requestId();
      String dependencyName = executeWithDependency.dependencyName();
      allInputNames.add(dependencyName);
      if (dependencyValuesCollector
              .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
              .putIfAbsent(dependencyName, executeWithDependency.results())
          != null) {
        throw new DuplicateRequestException(
            "Duplicate data for dependency %s of node %s in request %s"
                .formatted(dependencyName, nodeId, requestId));
      }
    }
  }

  private Map<String, List<NodeInputCommand>> execute(
      RequestId requestId, Set<String> newInputNames) {
    MainLogicDefinition<Object> mainLogicDefinition = nodeDefinition.getMainLogicDefinition();

    Map<String, InputValue<Object>> allInputs =
        inputsValueCollector.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    Map<String, Results<Object>> allDependencies =
        dependencyValuesCollector.computeIfAbsent(requestId, k -> new LinkedHashMap<>());
    ImmutableSet<String> allInputNames = mainLogicDefinition.inputNames();
    Set<String> availableInputs = Sets.union(allInputs.keySet(), allDependencies.keySet());
    if (availableInputs.isEmpty()) {
      if (!allInputNames.isEmpty()
          && nodeDefinition.resolverDefinitions().isEmpty()
          && !nodeDefinition.dependencyNodes().isEmpty()) {
        return executeDependenciesWhenNoResolvers(requestId);
      }
      return emptyMap();
    }

    Set<ResolverDefinition> pendingResolvers =
        getPendingResolvers(requestId, newInputNames, availableInputs);
    return executeResolvers(requestId, pendingResolvers, getMultiResolverDef());
  }

  /**
   * @param requestId The requestId.
   * @param newInputNames The input names for which new values were just made available.
   * @param availableInputs The inputs for which values are available.
   * @return the resolver definitions which need at least one of the provided {@code inputNames} and
   *     all of whose inputs' values are available. i.e resolvers which should be executed
   *     immediately
   */
  private Set<ResolverDefinition> getPendingResolvers(
      RequestId requestId, Set<String> newInputNames, Set<String> availableInputs) {
    Map<ResolverDefinition, ResolverCommand> resolverCommands =
        this.resolverResults.computeIfAbsent(requestId, r -> new LinkedHashMap<>());

    if (DependencyExecStrategy.INCREMENTAL.equals(dependencyExecStrategy)) {
      Set<ResolverDefinition> pendingUnboundResolvers =
          resolverDefinitionsByInput.getOrDefault(Optional.<String>empty(), emptyList()).stream()
              .filter(resolverDefinition -> !resolverCommands.containsKey(resolverDefinition))
              .filter(
                  resolverDefinition -> availableInputs.containsAll(resolverDefinition.boundFrom()))
              .collect(toSet());
      Set<ResolverDefinition> pendingResolvers =
          newInputNames.stream()
              .flatMap(
                  input ->
                      resolverDefinitionsByInput
                          .getOrDefault(ofNullable(input), ImmutableList.of())
                          .stream()
                          .filter(
                              resolverDefinition ->
                                  availableInputs.containsAll(resolverDefinition.boundFrom()))
                          .filter(
                              resolverDefinition ->
                                  !resolverCommands.containsKey(resolverDefinition)))
              .collect(toSet());
      pendingResolvers.addAll(pendingUnboundResolvers);
      return pendingResolvers;
    } else {
      return Stream.concat(
              Stream.of(Optional.<String>empty()), newInputNames.stream().map(Optional::of))
          .map(resolverDefinitionsByInput::get)
          .filter(Objects::nonNull)
          .flatMap(Collection::stream)
          .map(ResolverDefinition::dependencyName)
          .map(resolverDefinitionsByDependencies::get)
          .filter(
              resolverDefinitions ->
                  resolverDefinitions.stream()
                      .map(ResolverDefinition::boundFrom)
                      .flatMap(Collection::stream)
                      .allMatch(availableInputs::contains))
          .flatMap(Collection::stream)
          .filter(key -> !resolverCommands.containsKey(key))
          .collect(toSet());
    }
  }

  private Map<String, List<NodeInputCommand>> executeResolvers(
      RequestId requestId,
      Set<ResolverDefinition> pendingResolvers,
      Optional<MultiResolverDefinition> multiResolver) {
    if (multiResolver.isEmpty() || pendingResolvers.isEmpty()) {
      return emptyMap();
    }

    Map<String, List<ResolverDefinition>> resolversByDependency =
        pendingResolvers.stream().collect(groupingBy(ResolverDefinition::dependencyName));
    Map<String, ResolverCommand> resolverCommands =
        measuringTimeTaken(
            () -> {
              Optional<SkipNode> skipRequested =
                  this.skipLogicRequested.getOrDefault(requestId, Optional.empty());
              if (skipRequested.isPresent()) {
                SkipDependency skip =
                    ResolverCommand.skip(skipRequested.get().skipDependencyCommand().reason());
                return resolversByDependency.keySet().stream()
                    .collect(toMap(identity(), _k -> skip));
              } else {
                Inputs inputs =
                    getInputsFor(
                        requestId,
                        pendingResolvers.stream()
                            .map(ResolverDefinition::boundFrom)
                            .flatMap(Collection::stream)
                            .collect(toSet()));
                return multiResolver
                    .get()
                    .logic()
                    .resolve(
                        resolversByDependency.entrySet().stream()
                            .map(e -> new DependencyResolutionRequest(e.getKey(), e.getValue()))
                            .toList(),
                        inputs);
              }
            },
            timeTaken -> nodeMetrics.executeResolversTimeNs(timeTaken.toNanos()));

    Map<String, List<NodeInputCommand>> nodeRequestCommands = new LinkedHashMap<>();
    resolverCommands.forEach(
        (depName, resolverCommand) -> {
          nodeRequestCommands
              .computeIfAbsent(depName, _k -> new ArrayList<>())
              .addAll(
                  handleResolverCommand(
                      requestId, depName, resolversByDependency.get(depName), resolverCommand));
        });
    return nodeRequestCommands;
  }

  private List<NodeInputCommand> executeResolver(
      RequestId requestId, ResolverDefinition resolverDefinition) {
    NodeLogicId nodeLogicId = resolverDefinition.resolverNodeLogicId();
    ResolverCommand resolverCommand;
    Optional<SkipNode> skipRequested =
        this.skipLogicRequested.getOrDefault(requestId, Optional.empty());
    if (skipRequested.isPresent()) {
      resolverCommand = ResolverCommand.skip(skipRequested.get().skipDependencyCommand().reason());
    } else {
      Inputs inputsForResolver = getInputsForResolver(resolverDefinition, requestId);
      resolverCommand =
          nodeDefinition
              .nodeDefinitionRegistry()
              .logicDefinitionRegistry()
              .getResolver(nodeLogicId)
              .resolve(inputsForResolver);
    }
    String dependencyName = resolverDefinition.dependencyName();
    return handleResolverCommand(
        requestId, dependencyName, List.of(resolverDefinition), resolverCommand);
  }

  private static String getDependencyName(NodeRequestCommand nodeRequestCommand) {
    DependantChain dependantChain;
    if (nodeRequestCommand instanceof ExecuteWithInputs executeWithInputs) {
      dependantChain = executeWithInputs.dependantChain();
    } else if (nodeRequestCommand instanceof SkipNode skipNode) {
      dependantChain = skipNode.dependantChain();
    } else {
      throw new UnsupportedOperationException(
          "Unknown NodeRequestCommand Type: %s".formatted(nodeRequestCommand));
    }
    if (!(dependantChain instanceof DefaultDependantChain defaultDependantChain)) {
      throw new IllegalStateException("This should never happen");
    }
    return defaultDependantChain.dependencyName();
  }

  private List<NodeInputCommand> handleResolverCommand(
      RequestId requestId,
      String dependencyName,
      List<ResolverDefinition> resolverDefinitions,
      ResolverCommand resolverCommand) {
    return measuringTimeTaken(
        () ->
            _handleResolverCommand(requestId, dependencyName, resolverDefinitions, resolverCommand),
        timeTaken -> nodeMetrics.handleResolverCommandTimeNs(timeTaken.toNanos()));
  }

  private List<NodeInputCommand> _handleResolverCommand(
      RequestId requestId,
      String dependencyName,
      List<ResolverDefinition> resolverDefinitions,
      ResolverCommand resolverCommand) {

    List<NodeInputCommand> nodeRequestCommands = new ArrayList<>();
    NodeId depNodeId = nodeDefinition.dependencyNodes().get(dependencyName);
    Map<ResolverDefinition, ResolverCommand> resolverResults =
        this.resolverResults.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    resolverDefinitions.forEach(
        resolverDefinition -> resolverResults.put(resolverDefinition, resolverCommand));

    DependencyNodeExecutions dependencyNodeExecutions =
        dependencyExecutions
            .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
            .computeIfAbsent(dependencyName, k -> new DependencyNodeExecutions());
    dependencyNodeExecutions.executedResolvers().addAll(resolverDefinitions);
    if (resolverCommand instanceof SkipDependency) {
      if (dependencyValuesCollector.getOrDefault(requestId, ImmutableMap.of()).get(dependencyName)
          == null) {
        /* This is for the case where for some resolvers the input has already been resolved but we
        do need to skip them as well, as our current resolver is skipped.*/
        Set<RequestId> requestIdSet =
            new HashSet<>(dependencyNodeExecutions.individualCallResponses().keySet());
        /*Skipping Current resolver, as it's a skip, we don't need to iterate
         * over fanout requests as the input is empty*/
        requestIdSet.add(requestId.createNewRequest("%s[%s]".formatted(dependencyName, 0)));
        for (RequestId depRequestId : requestIdSet) {
          SkipNode skipNode =
              new SkipNode(
                  depNodeId,
                  depRequestId,
                  dependantChainByRequest
                      .getOrDefault(requestId, DependantChainStart.instance())
                      .extend(nodeId, dependencyName),
                  (SkipDependency) resolverCommand);
          nodeRequestCommands.add(skipNode);
        }
      }
    } else {
      // Since the resolver can return multiple inputs, we have to call the dependency Node
      // multiple times - each with a different request Id.
      // The current resolver  has triggered a fan-out.
      // So we need multiply the total number of requests to the dependency by n where n is
      // the size of the fan-out triggered by this resolver
      ImmutableList<Inputs> inputList = resolverCommand.getInputs();
      long executionsInProgress = dependencyNodeExecutions.executionCounter().longValue();
      Map<RequestId, Inputs> oldInputs = new LinkedHashMap<>();
      for (int i = 0; i < executionsInProgress; i++) {
        RequestId rid = requestId.createNewRequest("%s[%s]".formatted(dependencyName, i));
        oldInputs.put(
            rid,
            new Inputs(
                dependencyNodeExecutions
                    .individualCallInputs()
                    .getOrDefault(rid, Inputs.empty())
                    .values()));
      }

      long batchSize = max(executionsInProgress, 1);
      int requestCounter = 0;
      for (int j = 0; j < inputList.size(); j++) {
        Inputs inputs = inputList.get(j);
        for (int i = 0; i < batchSize; i++) {
          RequestId dependencyRequestId =
              requestId.createNewRequest("%s[%s]".formatted(dependencyName, j * batchSize + i));
          RequestId inProgressRequestId;
          if (executionsInProgress > 0) {
            inProgressRequestId = requestId.createNewRequest("%s[%s]".formatted(dependencyName, i));
          } else {
            inProgressRequestId = dependencyRequestId;
          }
          Inputs oldInput = oldInputs.getOrDefault(inProgressRequestId, Inputs.empty());
          if (requestCounter >= executionsInProgress) {
            dependencyNodeExecutions.executionCounter().increment();
          }
          Inputs newInputs;
          if (j == 0) {
            newInputs = inputs;
          } else {
            newInputs = Inputs.union(oldInput.values(), inputs.values());
          }
          dependencyNodeExecutions.individualCallInputs().put(dependencyRequestId, newInputs);
          ExecuteWithInputs nodeCommand =
              new ExecuteWithInputs(
                  depNodeId,
                  newInputs.values().keySet(),
                  newInputs,
                  dependantChainByRequest
                      .getOrDefault(requestId, DependantChainStart.instance())
                      .extend(nodeId, dependencyName),
                  dependencyRequestId);
          nodeRequestCommands.add(nodeCommand);
        }
        requestCounter += batchSize;
      }
    }
    //    handlePostResolution(requestId, dependencyName, depNodeId);
    return nodeRequestCommands;
  }

  private void registerDependencyCallbacks(
      RequestId requestId,
      String dependencyName,
      NodeId depNodeId,
      DependencyNodeExecutions dependencyNodeExecutions) {
    ImmutableSet<ResolverDefinition> resolverDefinitionsForDependency =
        this.resolverDefinitionsByDependencies.getOrDefault(dependencyName, ImmutableSet.of());
    if (resolverDefinitionsForDependency.equals(dependencyNodeExecutions.executedResolvers())) {
      allOf(
              dependencyNodeExecutions
                  .individualCallResponses()
                  .values()
                  .toArray(CompletableFuture[]::new))
          .whenComplete(
              (unused, throwable) -> {
                enqueueOrExecuteCommand(
                    () -> {
                      Results<Object> results;
                      if (throwable != null) {
                        results =
                            new Results<>(ImmutableMap.of(Inputs.empty(), withError(throwable)));
                      } else {
                        results =
                            new Results<>(
                                dependencyNodeExecutions.individualCallResponses().values().stream()
                                    .map(cf -> cf.getNow(new NodeResponse(requestId)))
                                    .collect(
                                        toImmutableMap(
                                            NodeResponse::inputs, NodeResponse::response)));
                      }
                      return new ExecuteWithDependency(
                          this.nodeId, dependencyName, results, requestId);
                    },
                    depNodeId);
              });

      flushDependencyIfNeeded(
          dependencyName,
          dependantChainByRequest.getOrDefault(requestId, DependantChainStart.instance()));
    }
  }

  private void registerBatchDependencyCallbacks(
      Collection<RequestId> requestIds,
      String dependencyName,
      NodeId depNodeId,
      BatchCommand<?> batchCommand) {
    ImmutableSet<ResolverDefinition> resolverDefinitionsForDependency =
        this.resolverDefinitionsByDependencies.getOrDefault(dependencyName, ImmutableSet.of());
    boolean allResolversExecuted =
        requestIds.stream()
            .map(
                requestId ->
                    dependencyExecutions
                        .getOrDefault(requestId, ImmutableMap.of())
                        .getOrDefault(dependencyName, new DependencyNodeExecutions()))
            .allMatch(
                dependencyNodeExecutions ->
                    resolverDefinitionsForDependency.equals(
                        dependencyNodeExecutions.executedResolvers()));
    if (allResolversExecuted) {
      allOf(
              requestIds.stream()
                  .map(
                      requestId ->
                          dependencyExecutions
                              .getOrDefault(requestId, ImmutableMap.of())
                              .getOrDefault(dependencyName, new DependencyNodeExecutions()))
                  .map(DependencyNodeExecutions::individualCallResponses)
                  .map(Map::values)
                  .flatMap(Collection::stream)
                  .toArray(CompletableFuture[]::new))
          .orTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
          .whenComplete(
              (unused, throwable) -> {
                enqueueOrExecuteBatchCommand(
                    () -> {
                      Map<RequestId, ExecuteWithDependency> callbacks = new LinkedHashMap<>();
                      for (RequestId requestId : requestIds) {
                        Results<Object> results;
                        if (throwable != null) {
                          results =
                              new Results<>(ImmutableMap.of(Inputs.empty(), withError(throwable)));
                        } else {
                          DependencyNodeExecutions dependencyNodeExecutions =
                              dependencyExecutions
                                  .getOrDefault(requestId, ImmutableMap.of())
                                  .getOrDefault(dependencyName, new DependencyNodeExecutions());
                          results =
                              new Results<>(
                                  dependencyNodeExecutions
                                      .individualCallResponses()
                                      .values()
                                      .stream()
                                      .map(cf -> cf.getNow(new NodeResponse(requestId)))
                                      .collect(
                                          toImmutableMap(
                                              NodeResponse::inputs, NodeResponse::response)));
                        }
                        callbacks.put(
                            requestId,
                            new ExecuteWithDependency(
                                this.nodeId, dependencyName, results, requestId));
                      }
                      return new DependencyCallbackBatch(
                          nodeId, callbacks, batchCommand.dependantChain());
                    },
                    depNodeId);
              });

      flushDependencyIfNeeded(dependencyName, batchCommand.dependantChain());
    }
  }

  private void enqueueOrExecuteCommand(
      Supplier<NodeRequestCommand> commandGenerator, NodeId depNodeId) {
    MainLogicDefinition<Object> depMainLogic =
        nodeDefinition.nodeDefinitionRegistry().get(depNodeId).getMainLogicDefinition();
    if (depMainLogic instanceof IOLogicDefinition<Object>) {
      krystalNodeExecutor.enqueueNodeCommand(commandGenerator);
    } else if (depMainLogic instanceof ComputeLogicDefinition<Object>) {
      krystalNodeExecutor.executeCommand(commandGenerator.get());
    } else {
      throw new UnsupportedOperationException(
          "Unknown logicDefinition type %s".formatted(depMainLogic.getClass()));
    }
  }

  private void enqueueOrExecuteBatchCommand(
      Supplier<BatchCommand<?>> commandGenerator, NodeId depNodeId) {
    MainLogicDefinition<Object> depMainLogic =
        nodeDefinition.nodeDefinitionRegistry().get(depNodeId).getMainLogicDefinition();
    if (depMainLogic instanceof IOLogicDefinition<Object>) {
      krystalNodeExecutor.enqueueNodeBatchCommand(commandGenerator);
    } else if (depMainLogic instanceof ComputeLogicDefinition<Object>) {
      krystalNodeExecutor.executeBatchCommand(commandGenerator.get());
    } else {
      throw new UnsupportedOperationException(
          "Unknown logicDefinition type %s".formatted(depMainLogic.getClass()));
    }
  }

  private void flushAllDependenciesIfNeeded(DependantChain dependantChain) {
    nodeDefinition
        .dependencyNodes()
        .keySet()
        .forEach(dependencyName -> flushDependencyIfNeeded(dependencyName, dependantChain));
  }

  private void flushDependencyIfNeeded(String dependencyName, DependantChain dependantChain) {
    if (!flushedDependantChain.getOrDefault(dependantChain, false)) {
      return;
    }
    Set<RequestId> requestsForDependantChain =
        requestsByDependantChain.getOrDefault(dependantChain, ImmutableSet.of());
    ImmutableSet<ResolverDefinition> resolverDefinitionsForDependency =
        this.resolverDefinitionsByDependencies.get(dependencyName);
    if (!requestsForDependantChain.isEmpty()
        && requestsForDependantChain.stream()
            .map(
                requestId ->
                    dependencyExecutions
                        .getOrDefault(requestId, ImmutableMap.of())
                        .getOrDefault(dependencyName, new DependencyNodeExecutions()))
            .allMatch(
                dependencyNodeExecutions ->
                    resolverDefinitionsForDependency.equals(
                        dependencyNodeExecutions.executedResolvers()))) {
      krystalNodeExecutor.executeCommand(
          new Flush(
              nodeDefinition.dependencyNodes().get(dependencyName),
              dependantChain.extend(nodeId, dependencyName)));
    }
  }

  private Inputs getInputsForResolver(ResolverDefinition resolverDefinition, RequestId requestId) {
    ImmutableSet<String> boundFrom = resolverDefinition.boundFrom();
    return getInputsFor(requestId, boundFrom);
  }

  private Inputs getInputsFor(RequestId requestId, Set<String> boundFrom) {
    Map<String, InputValue<Object>> allInputs =
        inputsValueCollector.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    Map<String, InputValue<Object>> inputValues = new LinkedHashMap<>();
    for (String boundFromInput : boundFrom) {
      InputValue<Object> voe = allInputs.get(boundFromInput);
      if (voe == null) {
        inputValues.put(
            boundFromInput,
            dependencyValuesCollector
                .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
                .get(boundFromInput));
      } else {
        inputValues.put(boundFromInput, voe);
      }
    }
    return new Inputs(inputValues);
  }

  private Map<String, List<NodeInputCommand>> executeDependenciesWhenNoResolvers(
      RequestId requestId) {
    Map<String, List<NodeInputCommand>> nodeReqCommands =
        new LinkedHashMap<>(nodeDefinition.dependencyNodes().size());
    nodeDefinition
        .dependencyNodes()
        .forEach(
            (depName, depNodeId) -> {
              if (!dependencyValuesCollector
                  .getOrDefault(requestId, ImmutableMap.of())
                  .containsKey(depName)) {
                RequestId dependencyRequestId = requestId.createNewRequest("%s".formatted(depName));
                ExecuteWithInputs nodeCommand =
                    new ExecuteWithInputs(
                        depNodeId,
                        ImmutableSet.of(),
                        Inputs.empty(),
                        dependantChainByRequest.get(requestId).extend(nodeId, depName),
                        dependencyRequestId);
                nodeReqCommands.computeIfAbsent(depName, _k -> new ArrayList<>()).add(nodeCommand);
              }
            });
    return nodeReqCommands;
  }

  private Optional<CompletableFuture<NodeResponse>> executeMainLogicIfPossible(
      RequestId requestId) {
    return measuringTimeTaken(
        () -> {
          // If all the inputs and dependency values are available, then prepare run mainLogic
          MainLogicDefinition<Object> mainLogicDefinition = nodeDefinition.getMainLogicDefinition();
          ImmutableSet<String> inputNames = mainLogicDefinition.inputNames();
          Set<String> collect =
              new LinkedHashSet<>(
                  inputsValueCollector.getOrDefault(requestId, ImmutableMap.of()).keySet());
          collect.addAll(
              dependencyValuesCollector.getOrDefault(requestId, ImmutableMap.of()).keySet());
          if (collect.containsAll(
              inputNames)) { // All the inputs of the logic node have data present
            return Optional.of(executeMainLogic(requestId));
          }
          return Optional.empty();
        },
        timeTaken -> nodeMetrics.mainLogicIfPossibleTimeNs(timeTaken.toNanos()));
  }

  private CompletableFuture<Object> executeDecoratedMainLogic(
      Inputs inputs, MainLogicDefinition<Object> mainLogicDefinition, RequestId requestId) {
    SortedSet<MainLogicDecorator> sortedDecorators =
        getSortedDecorators(dependantChainByRequest.get(requestId));
    MainLogic<Object> logic = mainLogicDefinition::execute;

    for (MainLogicDecorator mainLogicDecorator : sortedDecorators) {
      logic = mainLogicDecorator.decorateLogic(logic, mainLogicDefinition);
    }
    MainLogic<Object> finalLogic = logic;
    return measuringTimeTaken(
        () -> finalLogic.execute(ImmutableList.of(inputs)).get(inputs),
        timeTaken -> nodeMetrics.executeMainLogicTimeNs(timeTaken.toNanos()));
  }

  private CompletableFuture<NodeResponse> executeMainLogic(RequestId requestId) {

    MainLogicDefinition<Object> mainLogicDefinition = nodeDefinition.getMainLogicDefinition();
    MainLogicInputs mainLogicInputs = getInputsForMainLogic(requestId);
    // Retrieve existing result from cache if result for this set of inputs has already been
    // calculated
    CompletableFuture<Object> resultFuture =
        resultsCache.get(mainLogicInputs.nonDependencyInputs());
    if (resultFuture == null) {
      resultFuture =
          executeDecoratedMainLogic(
              mainLogicInputs.allInputsAndDependencies(), mainLogicDefinition, requestId);
      resultsCache.put(mainLogicInputs.nonDependencyInputs(), resultFuture);
    }
    mainLogicExecuted.put(requestId, true);
    flushDecoratorsIfNeeded(dependantChainByRequest.get(requestId));
    return resultFuture
        .handle(ValueOrError::valueOrError)
        .thenApply(voe -> new NodeResponse(mainLogicInputs.nonDependencyInputs(), voe, requestId));
  }

  private CompletableFuture<Map<RequestId, NodeResponse>> executeMainLogic(
      List<RequestId> requestIds, DependantChain dependantChain) {

    MainLogicDefinition<Object> mainLogicDefinition = nodeDefinition.getMainLogicDefinition();

    Map<RequestId, MainLogicInputs> mainLogicInputsByReq = new LinkedHashMap<>();
    Map<RequestId, CompletableFuture<ValueOrError<Object>>> resultsByRequest =
        new LinkedHashMap<>();

    for (RequestId requestId : requestIds) {
      mainLogicInputsByReq.put(requestId, getInputsForMainLogic(requestId));
    }
    CompletableFuture<Map<RequestId, NodeResponse>> resultForBatch = new CompletableFuture<>();
    executeDecoratedMainLogic(
        mainLogicDefinition, mainLogicInputsByReq, resultsByRequest, dependantChain);

    allOf(resultsByRequest.values().toArray(CompletableFuture[]::new))
        .whenComplete(
            (unused, throwable) -> {
              Map<RequestId, NodeResponse> batchResponses = new LinkedHashMap<>();
              for (Entry<RequestId, MainLogicInputs> entry : mainLogicInputsByReq.entrySet()) {
                RequestId requestId = entry.getKey();
                CompletableFuture<ValueOrError<Object>> requestResult =
                    resultsByRequest.get(requestId);
                batchResponses.put(
                    requestId,
                    new NodeResponse(
                        entry.getValue().nonDependencyInputs(),
                        requestResult.getNow(empty()),
                        requestId));
              }
              resultForBatch.complete(batchResponses);
            });

    requestIds.forEach(requestId -> mainLogicExecuted.put(requestId, true));
    flushDecoratorsIfNeeded(dependantChain);
    return resultForBatch;
  }

  private void executeDecoratedMainLogic(
      MainLogicDefinition<Object> mainLogicDefinition,
      Map<RequestId, MainLogicInputs> mainLogicInputsByReq,
      Map<RequestId, CompletableFuture<ValueOrError<Object>>> resultsByRequest,
      DependantChain dependantChain) {
    NavigableSet<MainLogicDecorator> sortedDecorators = getSortedDecorators(dependantChain);
    MainLogic<Object> logic = mainLogicDefinition::execute;

    for (MainLogicDecorator mainLogicDecorator : sortedDecorators) {
      logic = mainLogicDecorator.decorateLogic(logic, mainLogicDefinition);
    }
    MainLogic<Object> finalLogic = logic;
    mainLogicInputsByReq.forEach(
        (requestId, mainLogicInputs) -> {
          // Retrieve existing result from cache if result for this set of inputs has already been
          // calculated
          CompletableFuture<Object> cachedResult =
              resultsCache.get(mainLogicInputs.nonDependencyInputs());
          if (cachedResult == null) {
            cachedResult =
                measuringTimeTaken(
                    () ->
                        finalLogic
                            .execute(ImmutableList.of(mainLogicInputs.allInputsAndDependencies()))
                            .values()
                            .iterator()
                            .next(),
                    timeTaken -> nodeMetrics.executeMainLogicTimeNs(timeTaken.toNanos()));
            resultsCache.put(mainLogicInputs.nonDependencyInputs(), cachedResult);
          }
          resultsByRequest.put(requestId, cachedResult.handle(ValueOrError::valueOrError));
        });
    nodeMetrics.executeMainLogicCount();
  }

  private MainLogicInputs getInputsForMainLogic(RequestId requestId) {
    Inputs inputValues =
        new Inputs(inputsValueCollector.getOrDefault(requestId, ImmutableMap.of()));
    Map<String, Results<Object>> dependencyValues =
        dependencyValuesCollector.getOrDefault(requestId, ImmutableMap.of());
    Inputs allInputsAndDependencies = Inputs.union(dependencyValues, inputValues.values());
    return new MainLogicInputs(inputValues, allInputsAndDependencies);
  }

  private void collectInputValues(
      List<ExecuteWithInputs> executeWithInputsBatch, DependantChain dependantChain) {
    Set<String> allInputNames =
        collectedInputNames.computeIfAbsent(dependantChain, _k -> new LinkedHashSet<>());
    for (ExecuteWithInputs executeWithInputs : executeWithInputsBatch) {
      RequestId requestId = executeWithInputs.requestId();
      ImmutableSet<String> inputNames = executeWithInputs.inputNames();
      allInputNames.addAll(inputNames);
      Inputs inputs = executeWithInputs.values();
      for (String inputName : inputNames) {
        if (inputsValueCollector
                .computeIfAbsent(requestId, r -> new LinkedHashMap<>())
                .putIfAbsent(inputName, inputs.getInputValue(inputName))
            != null) {
          throw new DuplicateRequestException(
              "Duplicate data for inputs %s of node %s in request %s"
                  .formatted(inputNames, nodeId, requestId));
        }
      }
    }
  }

  private NavigableSet<MainLogicDecorator> getSortedDecorators(DependantChain dependantChain) {

    MainLogicDefinition<Object> mainLogicDefinition = nodeDefinition.getMainLogicDefinition();
    Map<String, MainLogicDecorator> decorators =
        new LinkedHashMap<>(
            mainLogicDefinition.getSessionScopedLogicDecorators(nodeDefinition, dependantChain));
    // If the same decoratorType is configured for session and request scope, request scope
    // overrides session scope.
    decorators.putAll(
        requestScopedDecoratorsSupplier.apply(
            new LogicExecutionContext(
                nodeId,
                mainLogicDefinition.logicTags(),
                dependantChain,
                nodeDefinition.nodeDefinitionRegistry())));
    TreeSet<MainLogicDecorator> sortedDecorators =
        new TreeSet<>(logicDecorationOrdering.decorationOrder());
    sortedDecorators.addAll(decorators.values());
    return sortedDecorators;
  }

  private static ImmutableMapView<Optional<String>, List<ResolverDefinition>>
      createResolverDefinitionsByInputs(ImmutableList<ResolverDefinition> resolverDefinitions) {
    Map<Optional<String>, List<ResolverDefinition>> resolverDefinitionsByInput =
        new LinkedHashMap<>();
    resolverDefinitions.forEach(
        resolverDefinition -> {
          if (!resolverDefinition.boundFrom().isEmpty()) {
            resolverDefinition
                .boundFrom()
                .forEach(
                    input ->
                        resolverDefinitionsByInput
                            .computeIfAbsent(Optional.of(input), s -> new ArrayList<>())
                            .add(resolverDefinition));
          } else {
            resolverDefinitionsByInput
                .computeIfAbsent(Optional.empty(), s -> new ArrayList<>())
                .add(resolverDefinition);
          }
        });
    return ImmutableMapView.viewOf(resolverDefinitionsByInput);
  }

  private void measuringTimeTaken(Runnable runnable, Consumer<Duration> totalTimeNs) {
    measuringTimeTaken(
        () -> {
          runnable.run();
          return null;
        },
        totalTimeNs);
  }

  private <T> T measuringTimeTaken(Supplier<T> callable, Consumer<Duration> totalTimeNs) {
    long start = System.nanoTime();
    try {
      return callable.get();
    } finally {
      totalTimeNs.accept(Duration.ofNanos(System.nanoTime() - start));
    }
  }

  private record DependencyNodeExecutions(
      LongAdder executionCounter,
      Set<ResolverDefinition> executedResolvers,
      Map<RequestId, Inputs> individualCallInputs,
      Map<RequestId, CompletableFuture<NodeResponse>> individualCallResponses) {

    private DependencyNodeExecutions() {
      this(new LongAdder(), new LinkedHashSet<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }
  }

  private record MainLogicInputs(Inputs nonDependencyInputs, Inputs allInputsAndDependencies) {}
}
