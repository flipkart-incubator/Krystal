package com.flipkart.krystal.krystex.node;

import static com.flipkart.krystal.data.ValueOrError.error;
import static com.flipkart.krystal.data.ValueOrError.valueOrError;
import static com.flipkart.krystal.data.ValueOrError.withValue;
import static com.google.common.collect.Maps.filterKeys;
import static java.util.concurrent.CompletableFuture.allOf;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.ResolverDefinition;
import com.flipkart.krystal.krystex.ResultFuture;
import com.flipkart.krystal.krystex.commands.ExecuteInputless;
import com.flipkart.krystal.krystex.commands.ExecuteWithInput;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import com.flipkart.krystal.krystex.commands.SkipNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Node {

  private final NodeId nodeId;

  private final NodeDefinition nodeDefinition;

  private final KrystalNodeExecutor krystalNodeExecutor;

  private final ImmutableMap<NodeLogicId, ImmutableList<MainLogicDecorator<Object>>> nodeDecorators;

  private final ImmutableMap<Optional<String>, List<ResolverDefinition>> resolverDefinitionsByInput;

  /** {@link ValueOrError} for inputs. {@link Results} for dependencies */
  private final Map<RequestId, Map<String, InputValue<?>>> inputsValueCollector =
      new LinkedHashMap<>();

  /** A unique Result future for every requestId. */
  private final Map<RequestId, NodeResponseFuture> resultsByRequest = new LinkedHashMap<>();

  /**
   * A unique {@link ResultFuture} for every new set of NodeInputs. This acts as a cache so that the
   * same computation is not repeated multiple times .
   */
  private final Map<Inputs, NodeResponseFuture> resultsCache = new LinkedHashMap<>();

  private final Map<RequestId, Map<NodeLogicId, ResolverCommand>> resolverResults =
      new LinkedHashMap<>();

  public Node(
      NodeDefinition nodeDefinition,
      KrystalNodeExecutor krystalNodeExecutor,
      ImmutableMap<NodeLogicId, ImmutableList<MainLogicDecorator<Object>>> nodeDecorators) {
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
      MainLogicDefinition<Object> mainLogicNodeDefinition =
          nodeDefinition.nodeDefinitionRegistry().logicDefinitionRegistry().getMain(mainLogicNode);
      boolean executeMainLogic;
      if (nodeCommand instanceof SkipNode skipNode) {
        resultForRequest
            .responseFuture()
            .completeExceptionally(new SkipNodeException(skipNode.reason()));
        return resultForRequest;
      } else if (nodeCommand instanceof ExecuteInputless executeInputless) {
        executeMainLogic = executeNodeWithNoInputs(executeInputless, requestId);
      } else if (nodeCommand instanceof ExecuteWithInput executeWithInput) {
        executeMainLogic = executeWithInput(requestId, executeWithInput);
      } else {
        throw new UnsupportedOperationException(
            "Unknown type of nodeCommand: %s".formatted(nodeCommand));
      }
      if (executeMainLogic) {
        Inputs inputs = new Inputs(inputsValueCollector.getOrDefault(requestId, ImmutableMap.of()));
        // Retrieve existing result from cache if result for this set of inputs has already been
        // calculated
        NodeResponseFuture mainLogicResponse =
            resultsCache.computeIfAbsent(
                inputs,
                i -> {
                  NodeResponseFuture newResult = new NodeResponseFuture();
                  executeMainLogic(
                          inputs,
                          mainLogicNodeDefinition,
                          nodeDecorators.getOrDefault(mainLogicNode, ImmutableList.of()))
                      .whenComplete(
                          (r, t) -> {
                            //noinspection SuspiciousMethodCalls
                            newResult
                                .inputsFuture()
                                .complete(
                                    new Inputs(
                                        filterKeys(
                                            inputs.values(),
                                            input ->
                                                !nodeDefinition
                                                    .dependencyNodes()
                                                    .containsKey(input))));
                            if (t != null) {
                              newResult.responseFuture().completeExceptionally(t);
                            } else {
                              newResult.responseFuture().complete(r);
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

  private boolean executeNodeWithNoInputs(ExecuteInputless executeInputless, RequestId requestId) {
    MainLogicDefinition<Object> mainLogicNodeDefinition =
        nodeDefinition
            .nodeDefinitionRegistry()
            .logicDefinitionRegistry()
            .getMain(nodeDefinition.mainLogicNode());
    ImmutableSet<String> inputNames = mainLogicNodeDefinition.inputNames();
    if (inputNames.isEmpty()) {
      return true;
    } else if (nodeDefinition.resolverDefinitions().isEmpty()
        && !nodeDefinition.dependencyNodes().isEmpty()) {
      nodeDefinition
          .dependencyNodes()
          .forEach(
              (depName, nodeId) -> {
                RequestId dependencyRequestId =
                    requestId.append("(%s)%s[%s]".formatted(depName, nodeId, 0));
                if (!inputsValueCollector
                    .getOrDefault(requestId, ImmutableMap.of())
                    .containsKey(depName)) {
                  krystalNodeExecutor
                      .executeNode(nodeId, Inputs.empty(), dependencyRequestId)
                      .responseFuture()
                      .whenComplete(
                          (o, throwable) ->
                              krystalNodeExecutor.enqueueCommand(
                                  new ExecuteWithInput(
                                      executeInputless.nodeId(),
                                      depName,
                                      valueOrError(o, throwable),
                                      requestId)));
                }
              });
      return false;
    } else {
      return executeWithInput(requestId, (String) null);
    }
  }

  private boolean executeWithInput(RequestId requestId, ExecuteWithInput executeWithInput) {
    String input = executeWithInput.name();
    InputValue<?> inputValue = executeWithInput.inputValue();
    if (inputsValueCollector
            .computeIfAbsent(requestId, r -> new LinkedHashMap<>())
            .putIfAbsent(input, inputValue)
        != null) {
      throw new DuplicateInputForRequestException(
          "Duplicate input data for a request %s".formatted(requestId));
    }
    return executeWithInput(requestId, input);
  }

  private boolean executeWithInput(RequestId requestId, @Nullable String input) {
    Map<String, InputValue<?>> inputs =
        inputsValueCollector.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    MainLogicDefinition<Object> mainLogicNodeDefinition =
        nodeDefinition
            .nodeDefinitionRegistry()
            .logicDefinitionRegistry()
            .getMain(nodeDefinition.mainLogicNode());
    resultsByRequest.computeIfAbsent(requestId, r -> new NodeResponseFuture());
    Map<NodeLogicId, ResolverCommand> nodeResults =
        this.resolverResults.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    Iterable<ResolverDefinition> pendingResolvers =
        resolverDefinitionsByInput
                .getOrDefault(Optional.ofNullable(input), ImmutableList.of())
                .stream()
                .filter(
                    resolverDefinition1 ->
                        !nodeResults.containsKey(resolverDefinition1.resolverNodeLogicId()))
            ::iterator;
    int pendingResolverCount = 0;
    for (ResolverDefinition resolverDefinition : pendingResolvers) {
      pendingResolverCount++;
      String dependencyName = resolverDefinition.dependencyName();
      ImmutableSet<String> boundFrom = resolverDefinition.boundFrom();
      NodeLogicId nodeLogicId = resolverDefinition.resolverNodeLogicId();
      if (boundFrom.stream().allMatch(inputs::containsKey)) {
        Inputs inputResolverInputs = new Inputs(filterKeys(inputs, boundFrom::contains));
        ResolverCommand resolverCommand =
            nodeDefinition
                .nodeDefinitionRegistry()
                .logicDefinitionRegistry()
                .getResolver(nodeLogicId)
                .resolve(inputResolverInputs);
        nodeResults.put(nodeLogicId, resolverCommand);
        NodeId nodeId = nodeDefinition.dependencyNodes().get(dependencyName);
        int counter = 0;
        // Since the node can return multiple results, we have to call the dependency Node
        // multiple times - each with a different request Id.
        Map<RequestId, NodeResponseFuture> dependencyResults = new LinkedHashMap<>();
        for (Inputs nodeInputs : resolverCommand.getInputs()) {
          RequestId dependencyRequestId =
              requestId.append("(%s)%s[%s]".formatted(dependencyName, nodeId, counter++));
          for (String dependencyInput : resolverDefinition.resolvedInputNames()) {
            dependencyResults.putIfAbsent(
                dependencyRequestId,
                krystalNodeExecutor.enqueueCommand(
                    new ExecuteWithInput(
                        nodeId,
                        dependencyInput,
                        nodeInputs.getInputValue(dependencyInput),
                        dependencyRequestId)));
          }
        }
        if (resolverCommand instanceof ResolverCommand.SkipDependency) {
          krystalNodeExecutor.enqueueCommand(
              new ExecuteWithInput(this.nodeId, dependencyName, Results.empty(), requestId));
        } else {
          allOf(
                  dependencyResults.values().stream()
                      .map(NodeResponseFuture::responseFuture)
                      .toArray(CompletableFuture[]::new))
              .whenComplete(
                  (unused, throwable) -> {
                    ImmutableMap<Inputs, ValueOrError<Object>> aggregate = ImmutableMap.of();
                    if (throwable == null) {
                      aggregate =
                          dependencyResults.values().stream()
                              .collect(
                                  ImmutableMap.toImmutableMap(
                                      nrf -> nrf.inputsFuture().getNow(null),
                                      nrf -> {
                                        try {
                                          return withValue(nrf.responseFuture().getNow(null));
                                        } catch (Exception e) {
                                          return error(e);
                                        }
                                      }));
                    }
                    krystalNodeExecutor.enqueueCommand(
                        new ExecuteWithInput(
                            this.nodeId, dependencyName, new Results<>(aggregate), requestId));
                  });
        }
      }
    }
    boolean executeMainLogic = false;
    if (pendingResolverCount == 0) {
      ImmutableSet<String> inputNames = mainLogicNodeDefinition.inputNames();
      if (inputsValueCollector
          .getOrDefault(requestId, ImmutableMap.of())
          .keySet()
          .containsAll(inputNames)) { // All the inputs of the logic node have data present
        executeMainLogic = true;
      }
    }
    return executeMainLogic;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static CompletableFuture<?> executeMainLogic(
      Inputs inputs,
      MainLogicDefinition<Object> mainLogicDefinition,
      ImmutableList<MainLogicDecorator<Object>> mainLogicDecorators) {
    MainLogic<?> logic = mainLogicDefinition::execute;
    for (MainLogicDecorator mainLogicDecorator : mainLogicDecorators) {
      logic = mainLogicDecorator.decorateLogic(mainLogicDefinition, logic);
    }
    return logic.execute(ImmutableList.of(inputs)).get(inputs);
  }

  private static ImmutableMap<Optional<String>, List<ResolverDefinition>>
      createResolverDefinitionsByInputs(ImmutableList<ResolverDefinition> resolverDefinitions) {
    Map<Optional<String>, List<ResolverDefinition>> resolverDefinitionsByInput =
        new LinkedHashMap<>();
    resolverDefinitions.forEach(
        resolverDefinition -> {
          if (!resolverDefinition.boundFrom().isEmpty()) {
            resolverDefinition
                .boundFrom()
                .forEach(
                    input ->
                        resolverDefinitionsByInput
                            .computeIfAbsent(Optional.of(input), s -> new ArrayList<>())
                            .add(resolverDefinition));
          } else {
            resolverDefinitionsByInput
                .computeIfAbsent(Optional.empty(), s -> new ArrayList<>())
                .add(resolverDefinition);
          }
        });
    return ImmutableMap.copyOf(resolverDefinitionsByInput);
  }
}
