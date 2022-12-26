package com.flipkart.krystal.krystex.node;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.concurrent.CompletableFuture.allOf;

import com.flipkart.krystal.krystex.MultiResultFuture;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.ResolverDefinition;
import com.flipkart.krystal.krystex.ResultFuture;
import com.flipkart.krystal.krystex.SingleResultFuture;
import com.flipkart.krystal.krystex.SingleValue;
import com.flipkart.krystal.krystex.commands.Execute;
import com.flipkart.krystal.krystex.commands.ExecuteWithInput;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Node {

  private final NodeId clusterId;

  private final NodeDefinition nodeDefinition;

  private final KrystalNodeExecutor krystalNodeExecutor;

  private final ImmutableMap<NodeLogicId, ImmutableList<NodeDecorator<Object>>> nodeDecorators;

  private final ImmutableMap<String, List<ResolverDefinition>> resolverDefinitionsByInput;

  /** Single Result for inputs. MultiResult for dependencies */
  private final Map<RequestId, Map<String, SingleValue<?>>> inputsValueCollector = new HashMap<>();

  private final Map<RequestId, SingleResultFuture<Object>> resultFuture = new LinkedHashMap<>();

  private final Map<RequestId, Map<NodeLogicId, ResultFuture>> nodeResults = new LinkedHashMap<>();

  public Node(
      NodeDefinition nodeDefinition,
      KrystalNodeExecutor krystalNodeExecutor,
      ImmutableMap<NodeLogicId, ImmutableList<NodeDecorator<Object>>> nodeDecorators) {
    this.clusterId = nodeDefinition.nodeId();
    this.nodeDefinition = nodeDefinition;
    this.krystalNodeExecutor = krystalNodeExecutor;
    this.nodeDecorators = nodeDecorators;
    this.resolverDefinitionsByInput =
        createResolverDefinitionsByInputs(nodeDefinition.resolverDefinitions());
  }

  public SingleResultFuture<Object> executeCommand(NodeCommand nodeCommand) {
    RequestId requestId = nodeCommand.requestId();
    final SingleResultFuture<Object> result =
        resultFuture.computeIfAbsent(requestId, r -> new SingleResultFuture<>());
    try {
      NodeLogicId logicNode = nodeDefinition.logicNode();
      NodeLogicDefinition<Object> logicNodeLogicDefinition =
          nodeDefinition.nodeDefinitionRegistry().logicDefinitionRegistry().get(logicNode);
      boolean executeLogic = false;
      if (nodeCommand instanceof Execute) {
        ImmutableSet<String> inputNames = logicNodeLogicDefinition.inputNames();
        if (inputNames.isEmpty()) {
          executeLogic = true;
        } else {
          nodeDefinition
              .dependencyNodes()
              .forEach(
                  (depName, nodeId) -> {
                    if (!inputsValueCollector
                        .getOrDefault(requestId, ImmutableMap.of())
                        .containsKey(depName)) {
                      krystalNodeExecutor
                          .executeNode(
                              nodeId, new NodeInputs(), requestId.append("%s".formatted(nodeId)))
                          .whenComplete(
                              (o, throwable) -> {
                                krystalNodeExecutor.enqueueCommand(
                                    new ExecuteWithInput(
                                        nodeCommand.nodeId(),
                                        depName,
                                        new SingleValue<>(o, throwable),
                                        requestId));
                              });
                    }
                  });
        }
      } else if (nodeCommand instanceof ExecuteWithInput executeWithInput) {
        Map<NodeLogicId, ResultFuture> nodeResults =
            this.nodeResults.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
        String input = executeWithInput.input();
        SingleValue<?> inputValue = executeWithInput.inputValue();
        resultFuture.computeIfAbsent(requestId, r -> new SingleResultFuture<>());
        Map<String, SingleValue<?>> inputs =
            inputsValueCollector.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
        if (inputs.putIfAbsent(input, inputValue) != null) {
          throw new IllegalArgumentException("Duplicate input data for a request");
        }
        ImmutableList<ResolverDefinition> pendingResolvers =
            resolverDefinitionsByInput.getOrDefault(input, ImmutableList.of()).stream()
                .filter(
                    resolverDefinition ->
                        !nodeResults.containsKey(resolverDefinition.resolverNodeLogicId()))
                .collect(toImmutableList());
        for (ResolverDefinition resolverDefinition : pendingResolvers) {
          String dependencyName = resolverDefinition.dependencyName();
          ImmutableSet<String> boundFrom = resolverDefinition.boundFrom();
          NodeLogicId nodeLogicId = resolverDefinition.resolverNodeLogicId();
          if (boundFrom.stream().allMatch(inputs::containsKey)) {
            NodeInputs inputResolverInputs =
                new NodeInputs(ImmutableMap.copyOf(Maps.filterKeys(inputs, boundFrom::contains)));
            NodeLogicDefinition<Object> definition =
                nodeDefinition.nodeDefinitionRegistry().logicDefinitionRegistry().get(nodeLogicId);
            MultiResultFuture<NodeInputs> results =
                executeNodeLogic(
                    inputResolverInputs,
                    definition,
                    nodeDecorators.getOrDefault(nodeLogicId, ImmutableList.of()));
            nodeResults.put(nodeLogicId, results);
            NodeId nodeId = nodeDefinition.dependencyNodes().get(dependencyName);
            int counter = 0;
            // Since the node can return multiple results, we have to call the dependency Node
            // multiple
            // times - each with a different request Id.
            Map<RequestId, CompletableFuture<?>> dependencyResults = new LinkedHashMap<>();
            for (SingleResultFuture<NodeInputs> resolverResult : results.toSingleResults()) {
              RequestId dependencyRequestId =
                  requestId.append("(%s)%s[%s]".formatted(dependencyName, nodeId, counter++));
              resolverResult
                  .future()
                  .whenComplete(
                      (nodeInputs, t) -> {
                        if (nodeInputs == null) {
                          nodeInputs = new NodeInputs();
                        }
                        for (String dependencyInput : resolverDefinition.resolvedInputNames()) {
                          dependencyResults.putIfAbsent(
                              dependencyRequestId,
                              krystalNodeExecutor.enqueueCommand(
                                  new ExecuteWithInput(
                                      nodeId,
                                      dependencyInput,
                                      nodeInputs.getValue(dependencyInput),
                                      dependencyRequestId)));
                        }
                      });
            }
            allOf(dependencyResults.values().toArray(CompletableFuture[]::new))
                .whenComplete(
                    (unused, throwable) -> {
                      ImmutableList<?> aggregate = ImmutableList.of();
                      if (throwable == null) {
                        aggregate =
                            dependencyResults.values().stream()
                                .map(f -> f.getNow(null))
                                .collect(toImmutableList());
                      }
                      krystalNodeExecutor.enqueueCommand(
                          new ExecuteWithInput(
                              this.clusterId,
                              dependencyName,
                              new SingleValue<>(aggregate, throwable),
                              requestId));
                    });
          }
        }
        if (pendingResolvers.isEmpty()) {
          ImmutableSet<String> inputNames = logicNodeLogicDefinition.inputNames();
          if (inputsValueCollector
              .getOrDefault(requestId, ImmutableMap.of())
              .keySet()
              .containsAll(inputNames)) { // All the inputs of the logic node have data present
            executeLogic = true;
          }
        }
      }
      if (executeLogic) {
        executeNodeLogic(
                new NodeInputs(
                    ImmutableMap.copyOf(
                        inputsValueCollector.getOrDefault(requestId, ImmutableMap.of()))),
                logicNodeLogicDefinition,
                nodeDecorators.getOrDefault(logicNode, ImmutableList.of()))
            .future()
            .whenComplete(
                (o, t) -> {
                  if (t != null) {
                    result.future().completeExceptionally(t);
                  } else {
                    if (o.size() != 1) {
                      throw new AssertionError(
                          "This should not be possible. Logic node should always return exactly one result");
                    }
                    result.future().complete(o.stream().iterator().next());
                  }
                });
      }
    } catch (Exception e) {
      result.future().completeExceptionally(e);
    }
    return result;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static <T> MultiResultFuture<T> executeNodeLogic(
      NodeInputs inputs,
      NodeLogicDefinition<?> nodeLogicDefinition,
      ImmutableList<NodeDecorator<Object>> nodeDecorators) {
    NodeLogic<?> logic = nodeLogicDefinition.logic();
    for (NodeDecorator nodeDecorator : nodeDecorators) {
      logic = nodeDecorator.decorateLogic(nodeLogicDefinition, logic);
    }
    return (MultiResultFuture<T>) logic.apply(ImmutableList.of(inputs)).get(inputs);
  }

  private static ImmutableMap<String, List<ResolverDefinition>> createResolverDefinitionsByInputs(
      ImmutableList<ResolverDefinition> resolverDefinitions) {
    Map<String, List<ResolverDefinition>> resolverDefinitionsByInput = new HashMap<>();
    resolverDefinitions.forEach(
        resolverDefinition ->
            resolverDefinition
                .boundFrom()
                .forEach(
                    input ->
                        resolverDefinitionsByInput
                            .computeIfAbsent(input, s -> new ArrayList<>())
                            .add(resolverDefinition)));
    return ImmutableMap.copyOf(resolverDefinitionsByInput);
  }
}
