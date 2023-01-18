package com.flipkart.krystal.krystex.node;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.commands.ExecuteWithAllInputs;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import com.flipkart.krystal.krystex.commands.SkipNode;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.decoration.NodeExecutionContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

/** Default implementation of Krystal executor which */
@Slf4j
public final class KrystalNodeExecutor implements KrystalExecutor {

  private final NodeDefinitionRegistry nodeDefinitionRegistry;
  private final LogicDecorationOrdering logicDecorationOrdering;
  private final ExecutorService commandQueue;
  private final RequestId requestId;

  private final Map<String, Map<String, MainLogicDecorator>> requestScopedMainDecorators =
      new LinkedHashMap<>();
  private final NodeRegistry nodeRegistry = new NodeRegistry();

  public KrystalNodeExecutor(
      NodeDefinitionRegistry nodeDefinitionRegistry,
      LogicDecorationOrdering logicDecorationOrdering,
      String requestId) {
    this.nodeDefinitionRegistry = nodeDefinitionRegistry;
    this.logicDecorationOrdering = logicDecorationOrdering;
    this.commandQueue =
        Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                .setNameFormat("KrystalTaskExecutorMainThread-%s".formatted(requestId))
                .build());
    this.requestId = new RequestId(requestId);
  }

  private ImmutableMap<String, MainLogicDecorator> getRequestScopedDecorators(
      NodeDefinition nodeDefinition, List<NodeId> dependants) {
    MainLogicDefinition<?> mainLogicDefinition =
        nodeDefinitionRegistry.logicDefinitionRegistry().getMain(nodeDefinition.mainLogicNode());
    Map<String, MainLogicDecorator> decorators = new LinkedHashMap<>();
    mainLogicDefinition
        .getRequestScopedLogicDecoratorConfigs()
        .forEach(
            (s, decoratorConfig) -> {
              NodeExecutionContext nodeExecutionContext =
                  new NodeExecutionContext(
                      nodeDefinition.nodeId(),
                      mainLogicDefinition.logicTags(),
                      dependants,
                      nodeDefinitionRegistry);
              if (decoratorConfig.shouldDecorate().test(nodeExecutionContext)) {
                String instanceId =
                    decoratorConfig.instanceIdGenerator().apply(nodeExecutionContext);
                decorators.put(
                    s,
                    requestScopedMainDecorators
                        .computeIfAbsent(s, k -> new LinkedHashMap<>())
                        .computeIfAbsent(instanceId, k -> decoratorConfig.factory().apply(k)));
              }
            });
    return ImmutableMap.copyOf(decorators);
  }

  @Override
  public <T> CompletableFuture<T> executeNode(NodeId nodeId, Inputs inputs) {
    //noinspection unchecked
    return (CompletableFuture<T>) executeNode(nodeId, inputs, requestId);
  }

  public <T> CompletableFuture<T> executeNode(NodeId nodeId, Inputs inputs, String requestId) {
    //noinspection unchecked
    return (CompletableFuture<T>) executeNode(nodeId, inputs, new RequestId(requestId));
  }

  private CompletableFuture<?> executeNode(NodeId nodeId, Inputs inputs, RequestId requestId) {
    return enqueueCommand(new ExecuteWithAllInputs(nodeId, inputs, requestId, ImmutableList.of()))
        .responseFuture()
        .thenApply(
            valueOrError -> {
              if (valueOrError.error().isPresent()) {
                throw new RuntimeException(valueOrError.error().get());
              } else {
                return valueOrError.value().orElse(null);
              }
            });
  }

  NodeResponseFuture enqueueCommand(NodeCommand nodeCommand) {
    NodeResponseFuture result = new NodeResponseFuture();
    CompletableFuture<NodeResponseFuture> nodeResponseFutureCompletableFuture =
        supplyAsync(() -> execute(nodeCommand), commandQueue);
    nodeResponseFutureCompletableFuture.whenComplete(
        (nodeResponseFuture, e) -> {
          if (e == null) {
            nodeResponseFuture
                .responseFuture()
                .whenComplete(
                    (o, t) -> {
                      if (t == null) {
                        result.responseFuture().complete(o);
                      } else {
                        result.responseFuture().completeExceptionally(t);
                      }
                    });
          } else {
            result.responseFuture().completeExceptionally(e);
          }
        });
    return result;
  }

  private NodeResponseFuture execute(NodeCommand nodeCommand) {
    NodeId nodeId = nodeCommand.nodeId();
    Node node =
        nodeRegistry.createIfAbsent(
            nodeId,
            n -> {
              List<NodeId> dependants;
              if (nodeCommand instanceof ExecuteWithAllInputs executeWithAllInputs) {
                dependants = executeWithAllInputs.dependants();
              } else if (nodeCommand instanceof SkipNode) {
                dependants = Collections.emptyList();
              } else {
                throw new IllegalStateException();
              }
              NodeDefinition nodeDefinition = nodeDefinitionRegistry.get(n);
              return new Node(
                  nodeDefinition,
                  this,
                  getRequestScopedDecorators(nodeDefinition, dependants),
                  logicDecorationOrdering);
            });
    return node.executeCommand(nodeCommand);
  }

  /**
   * Stops this executor from executing any pending nodes immediately. Also prevents accepting new
   * requests.
   */
  @Override
  public void close() {
    this.commandQueue.shutdown();
  }
}
