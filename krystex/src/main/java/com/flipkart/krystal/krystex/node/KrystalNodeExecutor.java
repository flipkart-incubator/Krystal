package com.flipkart.krystal.krystex.node;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.MainLogicChain;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.commands.ExecuteWithAllInputs;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.LinkedHashMap;
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
      NodeLogicId nodeLogicId) {
    MainLogicDefinition<?> mainLogicDefinition =
        nodeDefinitionRegistry.logicDefinitionRegistry().getMain(nodeLogicId);
    Map<String, MainLogicDecorator> decorators = new LinkedHashMap<>();
    mainLogicDefinition
        .getRequestScopedLogicDecoratorConfigs()
        .forEach(
            (s, decoratorConfig) -> {
              if (decoratorConfig.shouldDecorate().test(mainLogicDefinition.logicTags())) {
                String instanceId =
                    decoratorConfig.instanceIdGenerator().apply(mainLogicDefinition.logicTags());
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

  CompletableFuture<?> executeNode(NodeId nodeId, Inputs inputs, RequestId requestId) {
    NodeResponseFuture responseFuture;
    if (inputs.values().isEmpty()) {
      responseFuture =
          this.enqueueCommand(
              new ExecuteWithAllInputs(nodeId, Inputs.empty(), requestId));
    } else {
      ExecuteWithAllInputs executeWithInputs =
          new ExecuteWithAllInputs(nodeId, inputs, requestId);
      responseFuture = enqueueCommand(executeWithInputs);
    }
    return responseFuture
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
              NodeDefinition nodeDefinition = nodeDefinitionRegistry.get(n);
              return new Node(
                  nodeDefinition,
                  this,
                  getRequestScopedDecorators(nodeDefinition.mainLogicNode()),
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
