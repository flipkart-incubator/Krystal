package com.flipkart.krystal.krystex;

import static com.flipkart.krystal.krystex.Node.createNode;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.flipkart.krystal.krystex.commands.DependencyInputDone;
import com.flipkart.krystal.krystex.commands.DependencyNodeDone;
import com.flipkart.krystal.krystex.commands.InitiateNode;
import com.flipkart.krystal.krystex.commands.NewDataFromDependency;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import com.flipkart.krystal.krystex.commands.ProvideInputValues;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/** Default implementation of Krystal executor which */
@Slf4j
public final class KrystalNodeExecutor implements KrystalExecutor {

  private final NodeRegistry nodeRegistry;

  private final BlockingQueue<NodeCommand> mainQueue = new LinkedBlockingDeque<>();

  private final Future<?> mainLoopTask;

  private boolean shutdownRequested;
  private boolean stopAcceptingRequests;

  private final Map<DecoratorKey, NodeDecorator<?>> requestScopedNodeDecorators = new HashMap<>();

  public KrystalNodeExecutor(NodeDefinitionRegistry nodeDefinitionRegistry, String requestId) {
    this.nodeRegistry = new NodeRegistry(nodeDefinitionRegistry);
    this.mainLoopTask =
        newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                    .setNameFormat("KrystalTaskExecutorMainThread-%s".formatted(requestId))
                    .build())
            .submit(this::mainLoop);
  }

  public <T> Node<T> execute(NodeDefinition<T> nodeDefinition) {
    if (stopAcceptingRequests) {
      throw new IllegalStateException("Krystal has stopped accepting new requests for execution");
    }
    // TODO Implement caching
    return initiateAndGetNode(nodeDefinition);
  }

  @Override
  public void provideInputsAndMarkDone(String nodeId, NodeInputs nodeInputs) {
    NodeDefinition<?> nodeDefinition = nodeRegistry.getNodeDefinitionRegistry().get(nodeId);
    Node<?> node = initiateAndGetNode(nodeDefinition);
    enqueueCommand(new ProvideInputValues(node, nodeInputs));
    nodeInputs
        .values()
        .keySet()
        .forEach(inputName -> enqueueCommand(new DependencyInputDone(node, inputName)));
  }

  private void mainLoop() {
    while (!shutdownRequested) {
      NodeCommand currentCommand;
      try {
        currentCommand = mainQueue.take();
      } catch (InterruptedException ignored) {
        continue;
      }
      try {
        Node<?> node = currentCommand.node();
        if (currentCommand instanceof InitiateNode) {
          if (node.wasInitiated()) {
            // TODO Emit a no-op metric that shows that a node was added to the task queue
            // unnecessarily
            continue;
          }
          if (!initiatePendingInputs(node)) {
            node.executeIfNoDependenciesAndMarkDone();
          }
        } else if (currentCommand instanceof NewDataFromDependency newDataFromDependency) {
          node.executeWithNewDataForDependencyNode(
              newDataFromDependency.dependencyNodeId(), newDataFromDependency.newData());
        } else if (currentCommand instanceof DependencyNodeDone dependencyNodeDone) {
          node.markDependencyNodeDone(dependencyNodeDone.depNodeId());
        } else if (currentCommand instanceof DependencyInputDone dependencyInputDone) {
          node.markInputDone(dependencyInputDone.inputName());
        } else if (currentCommand instanceof ProvideInputValues provideInputValues) {
          provideInputValues
              .nodeInputs()
              .values()
              .forEach(
                  (input, value) -> {
                    node.executeWithNewDataForInput(
                        input, ImmutableList.of(new SingleResult<>(completedFuture(value))));
                  });
        }
      } catch (Exception e) {
        log.error("Error while executing node Command %s".formatted(currentCommand), e);
      }
    }
  }

  private <T> Node<T> initiateAndGetNode(NodeDefinition<T> nodeDefinition) {
    Node<T> node =
        this.nodeRegistry.createIfAbsent(
            nodeDefinition.nodeId(),
            () -> createNode(nodeDefinition, getRequestScopedNodeDecorators(nodeDefinition)));
    initiate(node);
    return node;
  }

  private <T> ImmutableList<NodeDecorator<T>> getRequestScopedNodeDecorators(
      NodeDefinition<T> nodeDefinition) {
    ImmutableMap<String, Map<String, Supplier<NodeDecorator<T>>>> suppliers =
        nodeDefinition.getRequestScopedNodeDecoratorSuppliers();
    ImmutableMap<String, String> groupMemberships = nodeDefinition.getGroupMemberships();
    List<NodeDecorator<T>> decorators = new ArrayList<>();
    suppliers.forEach(
        (groupType, supplierMap) -> {
          String groupName = groupMemberships.get(groupType);
          if (groupName == null) {
            return;
          }
          supplierMap.forEach(
              (decoratorId, supplier) -> {
                DecoratorKey decoratorKey =
                    new DecoratorKey(new NodeGroupId(groupType, groupName), decoratorId);
                //noinspection unchecked
                NodeDecorator<T> nodeDecorator =
                    (NodeDecorator<T>) requestScopedNodeDecorators.get(decoratorKey);
                if (nodeDecorator == null) {
                  nodeDecorator = supplier.get();
                  requestScopedNodeDecorators.put(decoratorKey, nodeDecorator);
                }
                decorators.add(nodeDecorator);
              });
        });
    return ImmutableList.copyOf(decorators);
  }

  private void initiate(Node<?> node) {
    enqueueCommand(new InitiateNode(node));
  }

  private void executeWithNewData(
      Node<?> node, String depNodeId, Collection<? extends SingleResult<?>> results) {
    enqueueCommand(new NewDataFromDependency(node, depNodeId, ImmutableList.copyOf(results)));
  }

  private void markDependencyDone(Node<?> node, String depNodeId) {
    enqueueCommand(new DependencyNodeDone(node, depNodeId));
  }

  /**
   * @param node
   * @return true if {@code node} has dependencies, false otherwise.
   */
  private boolean initiatePendingInputs(Node<?> node) {
    ImmutableSet<String> inputNames = node.definition().inputNames();
    ImmutableSet<String> dependencyNodeIds =
        ImmutableSet.copyOf(node.definition().inputProviders().values());
    if (node.wereDependenciesInitiated()) {
      return !inputNames.isEmpty();
    }
    if (inputNames.isEmpty()) {
      node.markDependenciesInitiated();
      return false;
    }
    for (String depNodeId : dependencyNodeIds) {
      Node<?> depNode = nodeRegistry.get(depNodeId);
      if (depNode == null) {
        depNode = execute(nodeRegistry.getNodeDefinitionRegistry().get(depNodeId));
      } else if (!depNode.wasInitiated()) {
        initiate(depNode);
      }
      depNode.whenNewDataAvailable(
          singleResults -> executeWithNewData(node, depNodeId, singleResults));
      depNode.whenDone(() -> markDependencyDone(node, depNodeId));
    }
    node.markDependenciesInitiated();
    return true;
  }

  /**
   * Stops this executor from accepting new requests for node execution but continue execution of
   * submitted nodes.
   */
  public void stopAcceptingRequests() {
    this.stopAcceptingRequests = true;
  }

  /**
   * Stops this executor from executing any pending nodes immediately. Also prevents accepting new
   * requests.
   */
  @Override
  public void close() {
    stopAcceptingRequests();
    this.shutdownRequested = true;
    this.mainLoopTask.cancel(true);
  }

  private void enqueueCommand(NodeCommand nodeCommand) {
    mainQueue.add(nodeCommand);
  }

  private record DecoratorKey(NodeGroupId nodeGroupId, String nodeDecoratorId) {}
}
