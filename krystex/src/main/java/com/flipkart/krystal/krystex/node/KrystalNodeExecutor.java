package com.flipkart.krystal.krystex.node;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;

import com.flipkart.krystal.krystex.KrystalExecutor;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.ResolverDefinition;
import com.flipkart.krystal.krystex.SingleValue;
import com.flipkart.krystal.krystex.commands.Execute;
import com.flipkart.krystal.krystex.commands.ExecuteWithInput;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import com.flipkart.krystal.krystex.node.Node;
import com.flipkart.krystal.krystex.node.NodeDecorator;
import com.flipkart.krystal.krystex.node.NodeDefinition;
import com.flipkart.krystal.krystex.node.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeGroupId;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeInputs;
import com.flipkart.krystal.krystex.node.NodeLogicDefinition;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.flipkart.krystal.krystex.node.NodeRegistry;
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
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/** Default implementation of Krystal executor which */
@Slf4j
public final class KrystalNodeExecutor implements KrystalExecutor {

  private final NodeDefinitionRegistry nodeDefinitionRegistry;
  private final ExecutorService commandQueue;
  private final RequestId requestId;

  private final Map<DecoratorKey, NodeDecorator<?>> requestScopedNodeDecorators = new HashMap<>();
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

  private <T> ImmutableList<NodeDecorator<T>> getRequestScopedNodeDecorators(
      NodeLogicId nodeLogicId) {
    NodeLogicDefinition<?> nodeLogicDefinition =
        nodeDefinitionRegistry.logicDefinitionRegistry().get(nodeLogicId);
    List<? extends NodeDecorator<?>> decorators =
        nodeLogicDefinition.getRequestScopedNodeDecoratorFactories().values().stream()
            .map(Supplier::get)
            .toList();
    //noinspection unchecked
    return (ImmutableList<NodeDecorator<T>>) ImmutableList.copyOf(decorators);
  }

  @Override
  public <T> CompletableFuture<T> executeNode(NodeId nodeId, NodeInputs nodeInputs) {
    return executeNode(nodeId, nodeInputs, requestId);
  }

  @SuppressWarnings("unchecked")
  <T> CompletableFuture<T> executeNode(NodeId nodeId, NodeInputs nodeInputs, RequestId requestId) {
    if (nodeInputs.values().isEmpty()) {
      return (CompletableFuture<T>) this.enqueueCommand(new Execute(nodeId, requestId));
    }
    List<CompletableFuture<Object>> list = new ArrayList<>();
    for (Entry<String, SingleValue<?>> e : nodeInputs.values().entrySet()) {
      ExecuteWithInput executeWithInput =
          new ExecuteWithInput(nodeId, e.getKey(), e.getValue(), requestId);
      CompletableFuture<Object> objectCompletableFuture = enqueueCommand(executeWithInput);
      list.add(objectCompletableFuture);
    }
    return (CompletableFuture<T>) list.stream().findAny().orElseThrow();
  }

  public CompletableFuture<Object> enqueueCommand(NodeCommand nodeCommand) {
    return supplyAsync(() -> execute(nodeCommand), commandQueue).thenCompose(identity());
  }

  private CompletableFuture<Object> execute(NodeCommand nodeCommand) {
    NodeId nodeId = nodeCommand.nodeId();
    Node node =
        nodeRegistry.createIfAbsent(
            nodeId,
            n -> {
              NodeDefinition nodeDefinition = nodeDefinitionRegistry.get(n);
              ImmutableMap<NodeLogicId, ImmutableList<NodeDecorator<Object>>> nodeDecorators =
                  Stream.concat(
                          Stream.of(nodeDefinition.logicNode()),
                          nodeDefinition.resolverDefinitions().stream()
                              .map(ResolverDefinition::resolverNodeLogicId))
                      .collect(toImmutableMap(identity(), this::getRequestScopedNodeDecorators));
              return new Node(nodeDefinition, this, nodeDecorators);
            });
    return node.executeCommand(nodeCommand).future();
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
