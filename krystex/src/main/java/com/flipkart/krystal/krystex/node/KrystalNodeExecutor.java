package com.flipkart.krystal.krystex.node;

import static com.flipkart.krystal.utils.Futures.linkFutures;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.SingleThreadExecutorPool;
import com.flipkart.krystal.krystex.commands.ExecuteWithInputs;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.NodeRequestCommand;
import com.flipkart.krystal.krystex.decoration.InitiateActiveDepChains;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig.DecoratorContext;
import com.flipkart.krystal.utils.MultiLeasePool;
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
import lombok.extern.slf4j.Slf4j;

/** Default implementation of Krystal executor which */
@Slf4j
public final class KrystalNodeExecutor implements KrystalExecutor {

  private final NodeDefinitionRegistry nodeDefinitionRegistry;
  private final LogicDecorationOrdering logicDecorationOrdering;
  private final MultiLeasePool.Lease<ExecutorService> commandQueueLease;
  private final RequestId requestId;

  /** DecoratorType -> {InstanceId -> Decorator} */
  private final Map<String, Map<String, MainLogicDecorator>> requestScopedMainDecorators =
      new LinkedHashMap<>();

  private final NodeRegistry nodeRegistry = new NodeRegistry();
  private volatile boolean closed;
  private final Map<RequestId, List<NodeExecutionInfo>> allRequests = new LinkedHashMap<>();
  private final Map<RequestId, List<NodeExecutionInfo>> unFlushedRequests = new LinkedHashMap<>();
  private final Map<NodeId, Set<DependantChain>> dependantChainsPerNode = new LinkedHashMap<>();

  public KrystalNodeExecutor(
      NodeDefinitionRegistry nodeDefinitionRegistry,
      LogicDecorationOrdering logicDecorationOrdering,
      SingleThreadExecutorPool commandQueuePool,
      String requestId) {
    this.nodeDefinitionRegistry = nodeDefinitionRegistry;
    this.logicDecorationOrdering = logicDecorationOrdering;
    this.commandQueueLease = commandQueuePool.lease();
    this.requestId = new RequestId(requestId);
  }

  private ImmutableMap<String, MainLogicDecorator> getRequestScopedDecorators(
      LogicExecutionContext logicExecutionContext) {
    NodeId nodeId = logicExecutionContext.nodeId();
    NodeDefinition nodeDefinition = nodeDefinitionRegistry.get(nodeId);
    MainLogicDefinition<?> mainLogicDefinition =
        nodeDefinitionRegistry.logicDefinitionRegistry().getMain(nodeDefinition.mainLogicNode());
    Map<String, MainLogicDecorator> decorators = new LinkedHashMap<>();
    mainLogicDefinition
        .getRequestScopedLogicDecoratorConfigs()
        .forEach(
            (decoratorType, decoratorConfig) -> {
              if (decoratorConfig.shouldDecorate().test(logicExecutionContext)) {
                String instanceId =
                    decoratorConfig.instanceIdGenerator().apply(logicExecutionContext);
                MainLogicDecorator mainLogicDecorator =
                    requestScopedMainDecorators
                        .computeIfAbsent(decoratorType, k -> new LinkedHashMap<>())
                        .computeIfAbsent(
                            instanceId,
                            k ->
                                decoratorConfig
                                    .factory()
                                    .apply(
                                        new DecoratorContext(instanceId, logicExecutionContext)));
                mainLogicDecorator.executeCommand(
                    new InitiateActiveDepChains(
                        nodeId, ImmutableSet.copyOf(dependantChainsPerNode.get(nodeId))));
                decorators.put(decoratorType, mainLogicDecorator);
              }
            });
    return ImmutableMap.copyOf(decorators);
  }

  @Override
  public <T> CompletableFuture<T> executeNode(NodeId nodeId, Inputs inputs) {
    //noinspection unchecked
    return (CompletableFuture<T>) executeNode(nodeId, inputs, requestId);
  }

  @Override
  public <T> CompletableFuture<T> executeNode(NodeId nodeId, Inputs inputs, String requestId) {
    //noinspection unchecked
    return (CompletableFuture<T>) executeNode(nodeId, inputs, new RequestId(requestId));
  }

  private CompletableFuture<?> executeNode(NodeId nodeId, Inputs inputs, RequestId requestId) {
    if (closed) {
      throw new RejectedExecutionException("KrystalNodeExecutor is already closed");
    }
    return supplyAsync(
            () -> {
              createDependantNodes(nodeId, DependantChainStart.instance());
              CompletableFuture<Object> future = new CompletableFuture<>();
              NodeExecutionInfo nodeExecutionInfo = new NodeExecutionInfo(nodeId, inputs, future);
              allRequests.computeIfAbsent(requestId, r -> new ArrayList<>()).add(nodeExecutionInfo);
              unFlushedRequests
                  .computeIfAbsent(requestId, r -> new ArrayList<>())
                  .add(nodeExecutionInfo);
              return future;
            },
            commandQueueLease.get())
        .thenCompose(identity());
  }

  private void createDependantNodes(NodeId nodeId, DependantChain dependantChain) {
    NodeDefinition nodeDefinition = nodeDefinitionRegistry.get(nodeId);
    if (dependantChain.contains(nodeId)) {
      // There is a cyclic dependency for the given node. So we avoid creating nodes for
      // dependencies nodes to avoid infinite recursion of this method.
      //
      // This means 'dependantChainsPerNode' field of this class will not contain all possible
      // dependantChains (since there will be infinitely many of them). Instead, there will be
      // exactly one dependantChain which will have this node as a dependant, and this
      // dependantChain can be used to infer that there is a dependency recursion.
      //
      // The implication of this is that any LogicDecorators configured for this node or any
      // of its transitive dependencies, where such LogicDecorators  rely on the
      // 'InitiateActiveDepChains' command to initiate all possible active DependantChains might
      // not work as expected (For example, InputModulationDecorator of vajram-krystex library).
      //
      // It is the responsibility of users of the krystex library to make sure that either:
      // 1. Nodes which have LogicDecorators which depend on activeDepChains to be exhaustive,
      // should not have dependant chains containing loops, or ...
      // 2. If the above is not possible, then such LogicDecorators should gracefully handle the
      // scenario that InitiateActiveDepChains will not contain recursive active dependant
      // chains.
      dependantChainsPerNode
          .computeIfAbsent(nodeId, k -> new LinkedHashSet<>())
          .add(dependantChain);
    } else {
      nodeRegistry.createIfAbsent(
          nodeId,
          n ->
              new Node(
                  nodeDefinition, this, this::getRequestScopedDecorators, logicDecorationOrdering));
      ImmutableMap<String, NodeId> dependencyNodes = nodeDefinition.dependencyNodes();
      dependencyNodes.forEach(
          (dependencyName, depNodeId) ->
              createDependantNodes(
                  depNodeId, DependantChain.from(nodeId, dependencyName, dependantChain)));
      dependantChainsPerNode
          .computeIfAbsent(nodeId, k -> new LinkedHashSet<>())
          .add(dependantChain);
    }
  }

  CompletableFuture<NodeResponse> enqueueCommand(NodeRequestCommand nodeCommand) {
    return supplyAsync(
            () -> nodeRegistry.get(nodeCommand.nodeId()).executeRequestCommand(nodeCommand),
            commandQueueLease.get())
        .thenCompose(identity());
  }

  void enqueueCommand(Flush flush) {
    runAsync(() -> nodeRegistry.get(flush.nodeId()).executeCommand(flush), commandQueueLease.get());
  }

  public void flush() {
    runAsync(
        () -> {
          unFlushedRequests.forEach(
              (requestId, nodeExecutionInfos) -> {
                nodeExecutionInfos.forEach(
                    nodeExecutionInfo -> {
                      NodeId nodeId = nodeExecutionInfo.nodeId();
                      if (nodeExecutionInfo.future().isDone()) {
                        return;
                      }
                      NodeDefinition nodeDefinition = nodeDefinitionRegistry.get(nodeId);
                      CompletableFuture<Object> submissionResult =
                          enqueueCommand(
                                  new ExecuteWithInputs(
                                      nodeId,
                                      nodeDefinitionRegistry
                                          .logicDefinitionRegistry()
                                          .getMain(nodeDefinition.mainLogicNode())
                                          .inputNames()
                                          .stream()
                                          .filter(
                                              s -> !nodeDefinition.dependencyNodes().containsKey(s))
                                          .collect(toImmutableSet()),
                                      nodeExecutionInfo.inputs(),
                                      DependantChainStart.instance(),
                                      requestId))
                              .thenApply(NodeResponse::response)
                              .thenApply(
                                  valueOrError -> {
                                    if (valueOrError.error().isPresent()) {
                                      throw new RuntimeException(valueOrError.error().get());
                                    } else {
                                      return valueOrError.value().orElse(null);
                                    }
                                  });
                      linkFutures(submissionResult, nodeExecutionInfo.future());
                    });
              });
          unFlushedRequests.forEach(
              (requestId, nodeExecutionInfos) ->
                  nodeExecutionInfos.forEach(
                      nodeExecutionInfo -> enqueueCommand(new Flush(nodeExecutionInfo.nodeId()))));
          unFlushedRequests.clear();
        },
        commandQueueLease.get());
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
    supplyAsync(
        () ->
            allOf(
                    allRequests.values().stream()
                        .flatMap(
                            nodeExecutionInfos ->
                                nodeExecutionInfos.stream().map(NodeExecutionInfo::future))
                        .toArray(CompletableFuture[]::new))
                .whenComplete((unused, throwable) -> commandQueueLease.close()),
        commandQueueLease.get());
  }

  private record NodeExecutionInfo(
      NodeId nodeId, Inputs inputs, CompletableFuture<Object> future) {}
}
