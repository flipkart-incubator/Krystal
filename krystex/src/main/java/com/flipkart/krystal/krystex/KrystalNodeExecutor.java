package com.flipkart.krystal.krystex;

import static com.flipkart.krystal.krystex.Node.createNode;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.flipkart.krystal.krystex.commands.DependencyDone;
import com.flipkart.krystal.krystex.commands.InitiateNode;
import com.flipkart.krystal.krystex.commands.NewDataFromDependency;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import com.flipkart.krystal.krystex.commands.ProvideInputValues;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import lombok.Getter;

/** Default implementation of Krystal executor which */
public final class KrystalNodeExecutor implements KrystalExecutor {

  @Getter private final NodeRegistry nodeRegistry;

  private final BlockingQueue<NodeCommand> mainQueue = new LinkedBlockingDeque<>();

  private final Future<?> mainLoopTask;

  private boolean shutdownRequested;
  private boolean stopAcceptingRequests;

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
    Node<T> node =
        this.nodeRegistry.createIfAbsent(
            nodeDefinition.nodeId(), () -> createNode(nodeDefinition, emptyList()));
    initiate(node);
    return node;
  }

  public <T> Node<T> executeWithInputs(
      NodeDefinition<T> nodeDefinition, Map<String, ?> inputValues) {
    Node<T> node = execute(nodeDefinition);
    provideInputs(node, inputValues);
    return node;
  }

  public void provideInputs(Node<?> node, Map<String, ?> inputValues) {
    getCommandQueue().add(new ProvideInputValues(node, ImmutableMap.copyOf(inputValues)));
  }

  private void initiate(Node<?> node) {
    getCommandQueue().add(new InitiateNode(node));
  }

  private void executeWithNewData(
      Node<?> node, String depNodeId, Collection<? extends SingleResult<?>> results) {
    getCommandQueue()
        .add(new NewDataFromDependency(node, depNodeId, ImmutableList.copyOf(results)));
  }

  private void markDependencyDone(Node<?> node, String depNodeId) {
    getCommandQueue().add(new DependencyDone(node, depNodeId));
  }

  private void mainLoop() {
    while (!shutdownRequested) {
      try {
        NodeCommand currentCommand = mainQueue.take();
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
        } else if (currentCommand instanceof DependencyDone dependencyDone) {
          node.markDependencyNodeDone(dependencyDone.depNodeId());
        } else if (currentCommand instanceof ProvideInputValues provideInputValues) {
          provideInputValues
              .values()
              .forEach(
                  (input, value) -> {
                    node.executeWithNewDataForInput(
                        input, ImmutableList.of(new SingleResult<>(completedFuture(value))));
                    node.markInputDone(input);
                  });
        }
      } catch (InterruptedException ignored) {

      }
    }
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
      depNode.whenDone(() -> markDependencyDone(node, depNodeId));
      depNode.whenNewDataAvailable(
          singleResults -> executeWithNewData(node, depNodeId, singleResults));
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

  private Queue<NodeCommand> getCommandQueue() {
    return mainQueue;
  }
}
