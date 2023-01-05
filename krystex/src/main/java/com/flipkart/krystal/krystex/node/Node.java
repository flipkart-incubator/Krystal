package com.flipkart.krystal.krystex.node;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Maps.filterKeys;
import static java.util.concurrent.CompletableFuture.allOf;

import com.flipkart.krystal.krystex.MultiResultFuture;
import com.flipkart.krystal.krystex.MultiValue;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.ResolverDefinition;
import com.flipkart.krystal.krystex.ResultFuture;
import com.flipkart.krystal.krystex.SingleResultFuture;
import com.flipkart.krystal.krystex.SingleValue;
import com.flipkart.krystal.krystex.Value;
import com.flipkart.krystal.krystex.commands.Execute;
import com.flipkart.krystal.krystex.commands.ExecuteWithInput;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Node {

  private final NodeId nodeId;

  private final NodeDefinition nodeDefinition;

  private final KrystalNodeExecutor krystalNodeExecutor;

  private final ImmutableMap<NodeLogicId, ImmutableList<NodeDecorator<Object>>> nodeDecorators;

  private final ImmutableMap<String, List<ResolverDefinition>> resolverDefinitionsByInput;

  /** Single Result for inputs. MultiResult for dependencies */
  private final Map<RequestId, Map<String, Value>> inputsValueCollector = new LinkedHashMap<>();

  /** A unique Result future for every requestId. */
  private final Map<RequestId, NodeResponseFuture> resultsByRequest = new LinkedHashMap<>();

  /**
   * A unique {@link ResultFuture} for every new set of NodeInputs. This acts as a cache so that the
   * same computation is not repeated multiple times .
   */
  private final Map<NodeInputs, NodeResponseFuture> resultsCache = new LinkedHashMap<>();

  private final Map<RequestId, Map<NodeLogicId, ResultFuture>> nodeLogicResults =
      new LinkedHashMap<>();

  public Node(
      NodeDefinition nodeDefinition,
      KrystalNodeExecutor krystalNodeExecutor,
      ImmutableMap<NodeLogicId, ImmutableList<NodeDecorator<Object>>> nodeDecorators) {
    this.nodeId = nodeDefinition.nodeId();
    this.nodeDefinition = nodeDefinition;
    this.krystalNodeExecutor = krystalNodeExecutor;
    this.nodeDecorators = nodeDecorators;
    this.resolverDefinitionsByInput =
        createResolverDefinitionsByInputs(nodeDefinition.resolverDefinitions());
  }

  public NodeResponseFuture executeCommand(NodeCommand nodeCommand) {
    RequestId requestId = nodeCommand.requestId();
    final NodeResponseFuture resultForRequest =
        resultsByRequest.computeIfAbsent(requestId, r -> new NodeResponseFuture());
    try {
      NodeLogicId mainLogicNode = nodeDefinition.mainLogicNode();
      NodeLogicDefinition<Object> mainLogicNodeDefinition =
          nodeDefinition.nodeDefinitionRegistry().logicDefinitionRegistry().get(mainLogicNode);
      boolean executeMainLogic = false;
      if (nodeCommand instanceof Execute) {
        ImmutableSet<String> inputNames = mainLogicNodeDefinition.inputNames();
        if (inputNames.isEmpty()) {
          executeMainLogic = true;
        } else {
          nodeDefinition
              .dependencyNodes()
              .forEach(
                  (depName, nodeId) -> {
                    if (!inputsValueCollector
                        .getOrDefault(requestId, ImmutableMap.of())
                        .containsKey(depName)) {
                      krystalNodeExecutor
                          .executeNode(nodeId, new NodeInputs(), requestId.append(nodeId))
                          .responseFuture()
                          .whenComplete(
                              (o, throwable) ->
                                  krystalNodeExecutor.enqueueCommand(
                                      new ExecuteWithInput(
                                          nodeCommand.nodeId(),
                                          depName,
                                          new SingleValue<>(o, throwable),
                                          requestId)));
                    }
                  });
        }
      } else if (nodeCommand instanceof ExecuteWithInput executeWithInput) {
        Map<NodeLogicId, ResultFuture> nodeResults =
            this.nodeLogicResults.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
        String input = executeWithInput.input();
        Value inputValue = executeWithInput.inputValue();
        resultsByRequest.computeIfAbsent(requestId, r -> new NodeResponseFuture());
        Map<String, Value> inputs =
            inputsValueCollector.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
        {
          if (inputs.putIfAbsent(input, inputValue) != null) {
            throw new DuplicateInputForRequestException(
                "Duplicate input data for a request %s".formatted(requestId));
          }
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
                new NodeInputs(ImmutableMap.copyOf(filterKeys(inputs, boundFrom::contains)));
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
            // multiple times - each with a different request Id.
            Map<RequestId, NodeResponseFuture> dependencyResults = new LinkedHashMap<>();
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
            allOf(
                    dependencyResults.values().stream()
                        .map(NodeResponseFuture::responseFuture)
                        .toArray(CompletableFuture[]::new))
                .whenComplete(
                    (unused, throwable) -> {
                      ImmutableMap<NodeInputs, SingleValue<Object>> aggregate = ImmutableMap.of();
                      if (throwable == null) {
                        aggregate =
                            dependencyResults.values().stream()
                                .collect(
                                    ImmutableMap.toImmutableMap(
                                        nrf -> nrf.inputsFuture().getNow(null),
                                        nrf -> {
                                          try {
                                            return new SingleValue<>(
                                                nrf.responseFuture().getNow(null));
                                          } catch (Exception e) {
                                            return new SingleValue<>(null, e);
                                          }
                                        }));
                      }
                      krystalNodeExecutor.enqueueCommand(
                          new ExecuteWithInput(
                              this.nodeId, dependencyName, new MultiValue<>(aggregate), requestId));
                    });
          }
        }
        if (pendingResolvers.isEmpty()) {
          ImmutableSet<String> inputNames = mainLogicNodeDefinition.inputNames();
          if (inputsValueCollector
              .getOrDefault(requestId, ImmutableMap.of())
              .keySet()
              .containsAll(inputNames)) { // All the inputs of the logic node have data present
            executeMainLogic = true;
          }
        }
      }
      if (executeMainLogic) {
        NodeInputs inputs =
            new NodeInputs(
                ImmutableMap.copyOf(
                    inputsValueCollector.getOrDefault(requestId, ImmutableMap.of())));
        // Retrieve existing result from cache if result for this set of inputs has already been
        // calculated
        NodeResponseFuture mainLogicResponse =
            resultsCache.computeIfAbsent(
                inputs,
                i -> {
                  NodeResponseFuture newResult = new NodeResponseFuture();
                  executeNodeLogic(
                          inputs,
                          mainLogicNodeDefinition,
                          nodeDecorators.getOrDefault(mainLogicNode, ImmutableList.of()))
                      .future()
                      .whenComplete(
                          (o, t) -> {
                            newResult
                                .inputsFuture()
                                .complete(
                                    new NodeInputs(
                                        ImmutableMap.copyOf(
                                            filterKeys(
                                                inputs.values(),
                                                input ->
                                                    !nodeDefinition
                                                        .dependencyNodes()
                                                        .containsKey(input)))));
                            if (t != null) {
                              newResult.responseFuture().completeExceptionally(t);
                            } else {
                              if (o.size() != 1) {
                                throw new AssertionError(
                                    "This should not be possible. Logic node should always return exactly one result");
                              }
                              newResult.responseFuture().complete(o.stream().iterator().next());
                            }
                          });
                  return newResult;
                });
        mainLogicResponse
            .responseFuture()
            .whenComplete(
                (o, t) -> {
                  resultForRequest
                      .inputsFuture()
                      .complete(mainLogicResponse.inputsFuture().getNow(null));
                  if (t != null) {
                    resultForRequest.responseFuture().completeExceptionally(t);
                  } else {
                    resultForRequest.responseFuture().complete(o);
                  }
                });
      }
    } catch (DuplicateInputForRequestException e) {
      throw e;
    } catch (Exception e) {
      resultForRequest.responseFuture().obtrudeException(e);
    }
    return resultForRequest;
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
    Map<String, List<ResolverDefinition>> resolverDefinitionsByInput = new LinkedHashMap<>();
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
