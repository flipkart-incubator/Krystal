package com.flipkart.krystal.krystex.node;

import static com.flipkart.krystal.krystex.node.KrystalNodeExecutor.GraphTraversalStrategy.BREADTH;
import static com.flipkart.krystal.utils.Futures.linkFutures;
import static com.flipkart.krystal.utils.Futures.propagateCancellation;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Sets.union;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardBatchCommand;
import com.flipkart.krystal.krystex.commands.ForwardGranularCommand;
import com.flipkart.krystal.krystex.commands.GranularNodeCommand;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import com.flipkart.krystal.krystex.commands.NodeDataCommand;
import com.flipkart.krystal.krystex.commands.SkipGranularCommand;
import com.flipkart.krystal.krystex.decoration.InitiateActiveDepChains;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig.DecoratorContext;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.utils.MultiLeasePool;
import com.flipkart.krystal.utils.MultiLeasePool.Lease;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/** Default implementation of Krystal executor which */
@Slf4j
public final class KrystalNodeExecutor implements KrystalExecutor {

  public enum ResolverExecStrategy {
    SINGLE,
    MULTI
  }

  public enum NodeExecStrategy {
    GRANULAR,
    BATCH
  }

  public enum GraphTraversalStrategy {
    DEPTH,
    BREADTH
  }

  private final NodeDefinitionRegistry nodeDefinitionRegistry;
  private final KrystalNodeExecutorConfig executorConfig;
  private final Lease<? extends ExecutorService> commandQueueLease;
  private final String instanceId;
  /**
   * We need to have a list of request scope global decorators corresponding to each type, in case
   * we want to have a decorator of one type but based on some config in request, we want to choose
   * one. Ex : Logger, based on prod or preprod env if we want to choose different types of loggers
   * Error logger or info logger
   */
  private final ImmutableMap<
          String, // DecoratorType
          List<MainLogicDecoratorConfig>>
      requestScopedLogicDecoratorConfigs;

  private final Map<
          String, // DecoratorType
          Map<
              String, // InstanceId
              MainLogicDecorator>>
      requestScopedMainDecorators = new LinkedHashMap<>();

  private final NodeRegistry<?> nodeRegistry = new NodeRegistry<>();
  private final KrystalNodeExecutorMetrics krystalNodeMetrics;
  private volatile boolean closed;
  private final Map<RequestId, NodeResult> allRequests = new LinkedHashMap<>();
  private final Set<RequestId> unFlushedRequests = new LinkedHashSet<>();
  private final Map<NodeId, Set<DependantChain>> dependantChainsPerNode = new LinkedHashMap<>();

  public KrystalNodeExecutor(
      NodeDefinitionRegistry nodeDefinitionRegistry,
      MultiLeasePool<? extends ExecutorService> commandQueuePool,
      KrystalNodeExecutorConfig executorConfig,
      String instanceId) {
    this.nodeDefinitionRegistry = nodeDefinitionRegistry;
    this.executorConfig = executorConfig;
    this.commandQueueLease = commandQueuePool.lease();
    this.instanceId = instanceId;
    this.requestScopedLogicDecoratorConfigs =
        ImmutableMap.copyOf(executorConfig.requestScopedLogicDecoratorConfigs());
    this.krystalNodeMetrics = new KrystalNodeExecutorMetrics();
  }

  private ImmutableMap<String, MainLogicDecorator> getRequestScopedDecorators(
      LogicExecutionContext logicExecutionContext) {
    NodeId nodeId = logicExecutionContext.nodeId();
    NodeDefinition nodeDefinition = nodeDefinitionRegistry.get(nodeId);
    MainLogicDefinition<?> mainLogicDefinition = nodeDefinition.getMainLogicDefinition();
    Map<String, MainLogicDecorator> decorators = new LinkedHashMap<>();
    Stream.concat(
            mainLogicDefinition.getRequestScopedLogicDecoratorConfigs().entrySet().stream(),
            requestScopedLogicDecoratorConfigs.entrySet().stream())
        .forEach(
            entry -> {
              String decoratorType = entry.getKey();
              List<MainLogicDecoratorConfig> decoratorConfigList =
                  new ArrayList<>(entry.getValue());
              decoratorConfigList.forEach(
                  decoratorConfig -> {
                    String instanceId =
                        decoratorConfig.instanceIdGenerator().apply(logicExecutionContext);
                    if (decoratorConfig.shouldDecorate().test(logicExecutionContext)) {
                      MainLogicDecorator mainLogicDecorator =
                          requestScopedMainDecorators
                              .computeIfAbsent(decoratorType, t -> new LinkedHashMap<>())
                              .computeIfAbsent(
                                  instanceId,
                                  _i ->
                                      decoratorConfig
                                          .factory()
                                          .apply(
                                              new DecoratorContext(
                                                  instanceId, logicExecutionContext)));
                      mainLogicDecorator.executeCommand(
                          new InitiateActiveDepChains(
                              nodeId, ImmutableSet.copyOf(dependantChainsPerNode.get(nodeId))));
                      decorators.putIfAbsent(decoratorType, mainLogicDecorator);
                    }
                  });
            });
    return ImmutableMap.copyOf(decorators);
  }

  @Override
  public <T> CompletableFuture<T> executeNode(
      NodeId nodeId, Inputs inputs, NodeExecutionConfig executionConfig) {
    if (closed) {
      throw new RejectedExecutionException("KrystalNodeExecutor is already closed");
    }

    checkArgument(executionConfig != null, "executionConfig can not be null");

    String executionId = executionConfig.executionId();
    checkArgument(executionId != null, "executionConfig.executionId can not be null");
    RequestId requestId = new RequestId("%s:%s".formatted(instanceId, executionId));

    //noinspection unchecked
    return (CompletableFuture<T>)
        enqueueCommand( // Perform all datastructure manipulations in the command queue to avoid
                // multi-thread access
                () -> {
                  createDependencyNodes(
                      nodeId, nodeDefinitionRegistry.getDependantChainsStart(), executionConfig);
                  CompletableFuture<Object> future = new CompletableFuture<>();
                  if (allRequests.containsKey(requestId)) {
                    future.completeExceptionally(
                        new IllegalArgumentException(
                            "Received duplicate requests for same instanceId '%s' and execution Id '%s'"
                                .formatted(instanceId, executionId)));
                  } else {
                    allRequests.put(
                        requestId,
                        new NodeResult(nodeId, requestId, inputs, executionConfig, future));
                    unFlushedRequests.add(requestId);
                  }
                  return future;
                })
            .thenCompose(identity());
  }

  private void createDependencyNodes(
      NodeId nodeId, DependantChain dependantChain, NodeExecutionConfig executionConfig) {
    NodeDefinition nodeDefinition = nodeDefinitionRegistry.get(nodeId);
    // If a dependantChain is disabled, don't create that node and its dependency nodes
    if (!union(executorConfig.disabledDependantChains(), executionConfig.disabledDependantChains())
        .contains(dependantChain)) {
      createNodeIfAbsent(nodeId, nodeDefinition);
      ImmutableMap<String, NodeId> dependencyNodes = nodeDefinition.dependencyNodes();
      dependencyNodes.forEach(
          (dependencyName, depNodeId) ->
              createDependencyNodes(
                  depNodeId, dependantChain.extend(nodeId, dependencyName), executionConfig));
      dependantChainsPerNode
          .computeIfAbsent(nodeId, _n -> new LinkedHashSet<>())
          .add(dependantChain);
    }
  }

  @SuppressWarnings("unchecked")
  private void createNodeIfAbsent(NodeId nodeId, NodeDefinition nodeDefinition) {
    if (isGranular()) {
      ((NodeRegistry<GranularNode>) nodeRegistry)
          .createIfAbsent(
              nodeId,
              _n ->
                  new GranularNode(
                      nodeDefinition,
                      this,
                      this::getRequestScopedDecorators,
                      executorConfig.logicDecorationOrdering(),
                      executorConfig.resolverExecStrategy()));
    } else {
      ((NodeRegistry<BatchNode>) nodeRegistry)
          .createIfAbsent(
              nodeId,
              _n ->
                  new BatchNode(
                      nodeDefinition,
                      this,
                      this::getRequestScopedDecorators,
                      executorConfig.logicDecorationOrdering(),
                      executorConfig.resolverExecStrategy()));
    }
  }

  private boolean isGranular() {
    return NodeExecStrategy.GRANULAR.equals(executorConfig.nodeExecStrategy());
  }

  /**
   * Enqueues the provided NodeRequestCommand supplier into the command queue. This method is
   * intended to be called in threads other than the main thread of this KrystalNodeExecutor.(for
   * example IO reactor threads). When a non-blocking IO call is made by a Node, a callback is added
   * to the resulting CompletableFuture which generates an ExecuteWithDependency command for its
   * dependents. That is when this method is used - ensuring that all further processing of the
   * nodeCammand happens in the main thread.
   */
  <R extends NodeResponse> CompletableFuture<R> enqueueNodeCommand(
      Supplier<? extends NodeCommand> nodeCommand) {
    return enqueueCommand((Supplier<CompletableFuture<R>>) () -> _executeCommand(nodeCommand.get()))
        .thenCompose(identity());
  }

  /**
   * When using {@link GraphTraversalStrategy#DEPTH}, this method can be called only from the main
   * thread of this KrystalNodeExecutor. Calling this method from any other thread (for example: IO
   * reactor threads) will cause race conditions, multithreaded access of non-thread-safe data
   * structures, and resulting unspecified behaviour.
   *
   * <p>When using {@link GraphTraversalStrategy#DEPTH}, this is a more optimal version of {@link
   * #enqueueNodeCommand(Supplier)} as it bypasses the command queue for the special case that the
   * command is originating from the same main thread inside the command queue,thus avoiding the
   * pontentially unnecessary contention in the thread-safe structures inside the command queue.
   */
  <T extends NodeResponse> CompletableFuture<T> executeCommand(NodeCommand nodeCommand) {
    if (BREADTH.equals(executorConfig.graphTraversalStrategy())) {
      return enqueueNodeCommand(() -> nodeCommand);
    } else {
      krystalNodeMetrics.commandQueueBypassed();
      return _executeCommand(nodeCommand);
    }
  }

  private <R extends NodeResponse> CompletableFuture<R> _executeCommand(NodeCommand nodeCommand) {
    try {
      validate(nodeCommand);
    } catch (Throwable e) {
      return failedFuture(e);
    }
    if (nodeCommand instanceof NodeDataCommand dataCommand) {
      @SuppressWarnings("unchecked")
      Node<NodeDataCommand, R> node =
          (Node<NodeDataCommand, R>) nodeRegistry.get(nodeCommand.nodeId());
      return node.executeCommand(dataCommand);
    } else if (nodeCommand instanceof Flush flush) {
      nodeRegistry.get(flush.nodeId()).executeCommand(flush);
      return completedFuture(null);
    } else {
      throw new UnsupportedOperationException(
          "Unknown NodeCommand type %s".formatted(nodeCommand.getClass()));
    }
  }

  private void validate(NodeCommand nodeCommand) {
    if (nodeCommand instanceof GranularNodeCommand granularNodeCommand) {
      DependantChain dependantChain = null;
      RequestId requestId = granularNodeCommand.requestId();
      if (nodeCommand instanceof ForwardGranularCommand forwardGranularCommand) {
        dependantChain = forwardGranularCommand.dependantChain();
      } else if (nodeCommand instanceof SkipGranularCommand skipNode) {
        dependantChain = skipNode.dependantChain();
      }
      if (union(
              executorConfig.disabledDependantChains(),
              allRequests
                  .get(requestId.originatedFrom())
                  .executionConfig()
                  .disabledDependantChains())
          .contains(dependantChain)) {
        throw new DisabledDependantChainException(dependantChain);
      }
    } else if (nodeCommand instanceof Flush flush) {
      DependantChain dependantChain = flush.nodeDependants();
      if (executorConfig.disabledDependantChains().contains(dependantChain)) {
        throw new DisabledDependantChainException(dependantChain);
      }
      if (allRequests.values().stream()
          .map(NodeResult::executionConfig)
          .map(NodeExecutionConfig::disabledDependantChains)
          .allMatch(disableDepChains -> disableDepChains.contains(dependantChain))) {
        throw new DisabledDependantChainException(dependantChain);
      }
    }
  }

  public void flush() {
    enqueueRunnable(
        () -> {
          if (isGranular()) {
            unFlushedRequests.forEach(
                requestId -> {
                  NodeResult nodeResult = allRequests.get(requestId);
                  NodeId nodeId = nodeResult.nodeId();
                  if (nodeResult.future().isDone()) {
                    return;
                  }
                  NodeDefinition nodeDefinition = nodeDefinitionRegistry.get(nodeId);
                  submitGranular(requestId, nodeResult, nodeId, nodeDefinition);
                });
          } else {
            submitBatch(unFlushedRequests);
          }
          unFlushedRequests.stream()
              .map(requestId -> allRequests.get(requestId).nodeId())
              .distinct()
              .forEach(
                  nodeId ->
                      executeCommand(
                          new Flush(nodeId, nodeDefinitionRegistry.getDependantChainsStart())));
        });
  }

  private void submitGranular(
      RequestId requestId, NodeResult nodeResult, NodeId nodeId, NodeDefinition nodeDefinition) {
    CompletableFuture<Object> submissionResult =
        this.<GranularNodeResponse>executeCommand(
                new ForwardGranularCommand(
                    nodeId,
                    nodeDefinition.getMainLogicDefinition().inputNames().stream()
                        .filter(s -> !nodeDefinition.dependencyNodes().containsKey(s))
                        .collect(toImmutableSet()),
                    nodeResult.inputs(),
                    nodeDefinitionRegistry.getDependantChainsStart(),
                    requestId))
            .thenApply(GranularNodeResponse::response)
            .thenApply(
                valueOrError -> {
                  if (valueOrError.error().isPresent()) {
                    throw new RuntimeException(valueOrError.error().get());
                  } else {
                    return valueOrError.value().orElse(null);
                  }
                });
    linkFutures(submissionResult, nodeResult.future());
  }

  private void submitBatch(Set<RequestId> unFlushedRequests) {
    unFlushedRequests.stream()
        .map(allRequests::get)
        .collect(groupingBy(NodeResult::nodeId))
        .forEach(
            (nodeId, nodeResults) -> {
              CompletableFuture<BatchNodeResponse> batchResponseFuture =
                  this.executeCommand(
                      new ForwardBatchCommand(
                          nodeId,
                          nodeResults.stream()
                              .collect(toImmutableMap(NodeResult::requestId, NodeResult::inputs)),
                          ImmutableMap.of(),
                          nodeDefinitionRegistry.getDependantChainsStart()));
              batchResponseFuture
                  .thenApply(BatchNodeResponse::responses)
                  .whenComplete(
                      (responses, throwable) -> {
                        for (NodeResult nodeResult : nodeResults) {
                          if (throwable != null) {
                            nodeResult.future().completeExceptionally(throwable);
                          } else {
                            ValueOrError<Object> result =
                                responses.getOrDefault(nodeResult.inputs(), ValueOrError.empty());
                            nodeResult.future().complete(result.value().orElse(null));
                          }
                        }
                      });
              propagateCancellation(
                  allOf(
                      nodeResults.stream()
                          .map(NodeResult::future)
                          .toArray(CompletableFuture[]::new)),
                  batchResponseFuture);
            });
  }

  public KrystalNodeExecutorMetrics getKrystalNodeMetrics() {
    return krystalNodeMetrics;
  }

  /**
   * Prevents accepting new requests. For reasons of performance optimization, submitted requests
   * are executed in this method.
   */
  @Override
  public void close() {
    if (closed) {
      return;
    }
    this.closed = true;
    flush();
    enqueueCommand(
        () ->
            allOf(
                    allRequests.values().stream()
                        .map(NodeResult::future)
                        .toArray(CompletableFuture[]::new))
                .whenComplete((unused, throwable) -> commandQueueLease.close()));
  }

  private CompletableFuture<Void> enqueueRunnable(Runnable command) {
    return enqueueCommand(
        () -> {
          command.run();
          return null;
        });
  }

  private <T> CompletableFuture<T> enqueueCommand(Supplier<T> command) {
    return supplyAsync(
        () -> {
          krystalNodeMetrics.commandQueued();
          return command.get();
        },
        commandQueueLease.get());
  }

  private record NodeResult(
      NodeId nodeId,
      RequestId requestId,
      Inputs inputs,
      NodeExecutionConfig executionConfig,
      CompletableFuture<Object> future) {}
}
