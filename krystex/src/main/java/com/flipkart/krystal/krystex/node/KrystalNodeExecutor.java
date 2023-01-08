package com.flipkart.krystal.krystex.node;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.commands.ExecuteInputless;
import com.flipkart.krystal.krystex.commands.ExecuteWithInput;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/** Default implementation of Krystal executor which */
@Slf4j
public final class KrystalNodeExecutor implements KrystalExecutor {

  private final NodeDefinitionRegistry nodeDefinitionRegistry;
  private final ExecutorService commandQueue;
  private final RequestId requestId;

  private final Map<DecoratorKey, MainLogicDecorator<?>> requestScopedNodeDecorators =
      new HashMap<>();
  private final NodeRegistry nodeRegistry = new NodeRegistry();

  public KrystalNodeExecutor(NodeDefinitionRegistry nodeDefinitionRegistry, String requestId) {
    this.nodeDefinitionRegistry = nodeDefinitionRegistry;
    this.commandQueue =
        Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                .setNameFormat("KrystalTaskExecutorMainThread-%s".formatted(requestId))
                .build());
    this.requestId = new RequestId(requestId);
  }

  private <T> ImmutableList<MainLogicDecorator<T>> getRequestScopedNodeDecorators(
      NodeLogicId nodeLogicId) {
    MainLogicDefinition<?> mainLogicDefinition =
        nodeDefinitionRegistry.logicDefinitionRegistry().getMain(nodeLogicId);
    List<? extends MainLogicDecorator<?>> decorators =
        mainLogicDefinition.getRequestScopedNodeDecoratorFactories().values().stream()
            .map(Supplier::get)
            .toList();
    //noinspection unchecked
    return (ImmutableList<MainLogicDecorator<T>>) ImmutableList.copyOf(decorators);
  }

  @Override
  public <T> CompletableFuture<T> executeNode(NodeId nodeId, Inputs nodeInputs) {
    //noinspection unchecked
    return (CompletableFuture<T>) executeNode(nodeId, nodeInputs, requestId).responseFuture();
  }

  public <T> CompletableFuture<T> executeNode(NodeId nodeId, Inputs nodeInputs, String requestId) {
    //noinspection unchecked
    return (CompletableFuture<T>)
        executeNode(nodeId, nodeInputs, new RequestId(requestId)).responseFuture();
  }

  NodeResponseFuture executeNode(NodeId nodeId, Inputs nodeInputs, RequestId requestId) {
    if (nodeInputs.values().isEmpty()) {
      return this.enqueueCommand(new ExecuteInputless(nodeId, requestId));
    }
    List<NodeResponseFuture> list = new ArrayList<>();
    for (Entry<String, InputValue<?>> e : nodeInputs.values().entrySet()) {
      ExecuteWithInput executeWithInput =
          new ExecuteWithInput(nodeId, e.getKey(), e.getValue(), requestId);
      list.add(enqueueCommand(executeWithInput));
    }
    return list.stream().findAny().orElseThrow();
  }

  public NodeResponseFuture enqueueCommand(NodeCommand nodeCommand) {
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
                      result
                          .inputsFuture()
                          .complete(nodeResponseFuture.inputsFuture().getNow(null));
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
              ImmutableMap<NodeLogicId, ImmutableList<MainLogicDecorator<Object>>> nodeDecorators =
                  ImmutableMap.of(
                      nodeDefinition.mainLogicNode(),
                      getRequestScopedNodeDecorators(nodeDefinition.mainLogicNode()));
              return new Node(nodeDefinition, this, nodeDecorators);
            });
    return node.executeCommand(nodeCommand);
  }

  private record DecoratorKey(NodeGroupId nodeGroupId, String nodeDecoratorId) {}

  /**
   * Stops this executor from executing any pending nodes immediately. Also prevents accepting new
   * requests.
   */
  @Override
  public void close() {
    this.commandQueue.shutdown();
  }
}
