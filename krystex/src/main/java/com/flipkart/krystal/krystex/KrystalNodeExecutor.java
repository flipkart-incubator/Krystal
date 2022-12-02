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
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import lombok.Getter;

/**
 * Default implementation of Krystal executor which
 */
public final class KrystalNodeExecutor implements KrystalExecutor {

  @Getter
  private final NodeRegistry nodeRegistry;

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
      Node<?> node, String depName, String depNodeId,
      Collection<? extends SingleResult<?>> results) {
    getCommandQueue()
        .add(new NewDataFromDependency(node, depName, depNodeId, ImmutableList.copyOf(results)));
  }

  private void markDependencyDone(Node<?> node, String depNodeId, String depName) {
    getCommandQueue().add(new DependencyDone(node, depNodeId, depName));
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
          if (!initiatePendingDependencies(node)) {
            node.executeIfNoDependenciesAndInputsAndMarkDone();
          }
        } else if (currentCommand instanceof NewDataFromDependency newDataFromDependency) {
          node.executeWithNewDataFromDependencyNode(
              newDataFromDependency.dependencyNodeId(), newDataFromDependency.newData());
          notifyInputAdaptors(newDataFromDependency.node(),
              newDataFromDependency.depName(), newDataFromDependency.newData());
          node.executeWithNewDataForInput(newDataFromDependency.depName(), newDataFromDependency.newData());
        } else if (currentCommand instanceof DependencyDone dependencyDone) {
          node.markDependencyNodeDone(dependencyDone.depNodeId());
        } else if (currentCommand instanceof ProvideInputValues provideInputValues) {
          provideInputValues
              .values()
              .forEach(
                  (input, value) -> {
                    node.executeWithNewDataForInput(
                        input, ImmutableList.of(new SingleResult<>(completedFuture(value))));
                    triggerAdaptor(node,
                        input, value);
                    node.markInputDone(input);
                  });
        }
      } catch (InterruptedException ignored) {

      }
    }
  }

  private void notifyInputAdaptors(Node<?> node, String input,
      ImmutableCollection<SingleResult<?>> results) {
    List<Object> values = new LinkedList<>();
    results.forEach(result -> {
      try {
        values.add(result.future().get());
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    });
    if (!node.definition().inputAdaptionTarget(input).isEmpty()) {
      node.definition().inputAdaptionTarget(input).entrySet().forEach(inputToAdaptor -> {
        Map<String, Object> inputValues = new HashMap<>();
        inputValues.put(inputToAdaptor.getKey(), values.get(0));
        executeWithInputs(inputToAdaptor.getValue(), inputValues);
      });
    }
  }

  private void triggerAdaptor(Node<?> node, String input,
      Object value) {
    if (!node.definition().inputAdaptionTarget(input).isEmpty()) {
      node.definition().inputAdaptionTarget(input).entrySet().forEach(inputToAdaptor -> {
        Map<String, Object> inputValues = new HashMap<>();
        inputValues.put(inputToAdaptor.getKey(), value);
        executeWithInputs(inputToAdaptor.getValue(), inputValues);
      });
    }
  }

  /**
   * @param node
   * @return true if {@code node} has dependencies, false otherwise.
   */
  private boolean initiatePendingDependencies(Node<?> node) {
    ImmutableSet<String> dependencyNames = node.definition().dependencyNames();
    ImmutableMap<String, String> dependencyNameNodeIds =
        ImmutableMap.copyOf(node.definition().dependencyProviders());

    if (node.wereDependenciesInitiated()) {
      return !dependencyNames.isEmpty();
    }
    if (dependencyNames.isEmpty()) {
      node.markDependenciesInitiated();
      return false;
    }
    for (Map.Entry<String, String> depNameNodeId : dependencyNameNodeIds.entrySet()) {
      Node<?> depNode = nodeRegistry.get(depNameNodeId.getValue());
      if (depNode == null) {
        depNode = execute(nodeRegistry.getNodeDefinitionRegistry().get(depNameNodeId.getValue()));
      } else if (!depNode.wasInitiated()) {
        initiate(depNode);
      }
      depNode.whenDone(
          () -> markDependencyDone(node, depNameNodeId.getValue(), depNameNodeId.getKey()));

      depNode.whenNewDataAvailable(singleResults -> {
        if (!singleResults.isEmpty())
          executeWithNewData(node, depNameNodeId.getKey(), depNameNodeId.getValue(), singleResults);
      });
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