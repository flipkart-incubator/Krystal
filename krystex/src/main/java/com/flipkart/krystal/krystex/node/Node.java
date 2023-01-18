package com.flipkart.krystal.krystex.node;

import static com.flipkart.krystal.data.ValueOrError.empty;
import static com.flipkart.krystal.data.ValueOrError.error;
import static com.flipkart.krystal.utils.Futures.propagateCompletion;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Maps.filterKeys;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.ResolverCommand;
import com.flipkart.krystal.krystex.ResolverCommand.SkipDependency;
import com.flipkart.krystal.krystex.ResolverDefinition;
import com.flipkart.krystal.krystex.ResultFuture;
import com.flipkart.krystal.krystex.commands.ExecuteWithAllInputs;
import com.flipkart.krystal.krystex.commands.ExecuteWithDependency;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import com.flipkart.krystal.krystex.commands.SkipNode;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public class Node {

  private final NodeId nodeId;

  private final NodeDefinition nodeDefinition;

  private final KrystalNodeExecutor krystalNodeExecutor;

  private final ImmutableMap<String, MainLogicDecorator> requestScopedMainLogicDecorators;

  private final ImmutableMap<Optional<String>, List<ResolverDefinition>> resolverDefinitionsByInput;
  private final ImmutableMap<String, List<ResolverDefinition>> resolverDefinitionsByDependency;
  private final LogicDecorationOrdering logicDecorationOrdering;

  /** {@link ValueOrError} for inputs. {@link Results} for dependencies */
  private final Map<RequestId, Map<String, InputValue<Object>>> inputsValueCollector =
      new LinkedHashMap<>();

  /** A unique Result future for every requestId. */
  private final Map<RequestId, NodeResponseFuture> resultsByRequest = new LinkedHashMap<>();

  /**
   * A unique {@link ResultFuture} for every new set of Inputs. This acts as a cache so that the
   * same computation is not repeated multiple times .
   */
  private final Map<Inputs, CompletableFuture<ValueOrError<Object>>> resultsCache =
      new LinkedHashMap<>();

  private final Map<RequestId, Map<NodeLogicId, List<ResolverCommand>>> resolverResults =
      new LinkedHashMap<>();

  public Node(
      NodeDefinition nodeDefinition,
      KrystalNodeExecutor krystalNodeExecutor,
      ImmutableMap<String, MainLogicDecorator> requestScopedMainLogicDecorators,
      LogicDecorationOrdering logicDecorationOrdering) {
    this.nodeId = nodeDefinition.nodeId();
    this.nodeDefinition = nodeDefinition;
    this.krystalNodeExecutor = krystalNodeExecutor;
    this.requestScopedMainLogicDecorators = requestScopedMainLogicDecorators;
    this.resolverDefinitionsByInput =
        createResolverDefinitionsByInputs(nodeDefinition.resolverDefinitions());
    this.resolverDefinitionsByDependency =
        createResolverDefinitionsByDependency(nodeDefinition.resolverDefinitions());
    this.logicDecorationOrdering = logicDecorationOrdering;
  }

  NodeResponseFuture executeCommand(NodeCommand nodeCommand) {
    RequestId requestId = nodeCommand.requestId();
    final NodeResponseFuture resultForRequest =
        resultsByRequest.computeIfAbsent(requestId, r -> new NodeResponseFuture());
    try {
      boolean executeMainLogic;
      if (nodeCommand instanceof SkipNode skipNode) {
        resultForRequest
            .responseFuture()
            .completeExceptionally(
                new SkipNodeException(skipNode.skipDependencyCommand().reason()));
        return resultForRequest;
      } else if (nodeCommand instanceof ExecuteWithDependency executeWithInput) {
        executeMainLogic = executeWithInput(requestId, executeWithInput);
      } else if (nodeCommand instanceof ExecuteWithAllInputs executeWithAllInputs) {
        executeMainLogic = executeWithInputs(requestId, executeWithAllInputs);
      } else {
        throw new UnsupportedOperationException(
            "Unknown type of nodeCommand: %s".formatted(nodeCommand));
      }
      if (executeMainLogic) {
        executeMainLogic(resultForRequest, requestId);
      }
    } catch (DuplicateInputForRequestException e) {
      throw e;
    } catch (Exception e) {
      resultForRequest.responseFuture().completeExceptionally(e);
    }
    return resultForRequest;
  }

  private boolean executeWithInputs(
      RequestId requestId, ExecuteWithAllInputs executeWithAllInputs) {
    Set<String> providedInputs =
        new LinkedHashSet<>(executeWithAllInputs.inputs().values().keySet());
    nodeDefinition
        .nodeDefinitionRegistry()
        .logicDefinitionRegistry()
        .getMain(nodeDefinition.mainLogicNode())
        .inputNames()
        .stream()
        .filter(s -> !nodeDefinition.dependencyNodes().containsKey(s))
        .forEach(providedInputs::add);
    ImmutableSet<String> inputNames = ImmutableSet.copyOf(providedInputs);
    collectInputValues(requestId, inputNames, executeWithAllInputs.inputs());
    return executeWithInputs(requestId, inputNames, executeWithAllInputs);
  }

  private boolean executeWithInput(RequestId requestId, ExecuteWithDependency executeWithInput) {
    String input = executeWithInput.dependencyName();
    ImmutableSet<String> inputNames = ImmutableSet.of(input);
    collectInputValues(
        requestId, inputNames, new Inputs(ImmutableMap.of(input, executeWithInput.results())));
    return executeWithInputs(requestId, inputNames, executeWithInput);
  }

  private void collectInputValues(
      RequestId requestId, ImmutableSet<String> inputNames, Inputs executeWithInput) {
    for (String inputName : inputNames) {
      if (inputsValueCollector
              .computeIfAbsent(requestId, r -> new LinkedHashMap<>())
              .putIfAbsent(inputName, executeWithInput.values().get(inputName))
          != null) {
        throw new DuplicateInputForRequestException(
            "Duplicate input data for a request %s".formatted(requestId));
      }
    }
  }

  private boolean executeWithInputs(
      RequestId requestId, ImmutableSet<String> newInputNames, NodeCommand nodeCommand) {
    MainLogicDefinition<Object> mainLogicNodeDefinition =
        nodeDefinition
            .nodeDefinitionRegistry()
            .logicDefinitionRegistry()
            .getMain(nodeDefinition.mainLogicNode());
    Inputs allInputs =
        new Inputs(inputsValueCollector.computeIfAbsent(requestId, r -> new LinkedHashMap<>()));
    ImmutableSet<String> inputAndDepNames = mainLogicNodeDefinition.inputNames();
    if (allInputs.values().isEmpty()) {
      if (inputAndDepNames.isEmpty()) {
        return true;
      } else if (nodeDefinition.resolverDefinitions().isEmpty()
          && !nodeDefinition.dependencyNodes().isEmpty()) {
        nodeDefinition
            .dependencyNodes()
            .forEach(
                (depName, depNodeId) -> {
                  if (!inputsValueCollector
                      .getOrDefault(requestId, ImmutableMap.of())
                      .containsKey(depName)) {
                    RequestId dependencyRequestId = requestId.append("%s".formatted(depName));
                    NodeResponseFuture nodeResponse =
                        krystalNodeExecutor.enqueueCommand(
                            new ExecuteWithAllInputs(
                                depNodeId, Inputs.empty(), dependencyRequestId));
                    nodeResponse
                        .responseFuture()
                        .whenComplete(
                            (result, throwable) -> {
                              if (throwable != null) {
                                result = error(throwable);
                              }
                              krystalNodeExecutor.enqueueCommand(
                                  new ExecuteWithDependency(
                                      nodeCommand.nodeId(),
                                      depName,
                                      new Results<>(ImmutableMap.of(Inputs.empty(), result)),
                                      requestId));
                            });
                  }
                });
        return false;
      }
    }
    Set<String> allInputNames = allInputs.values().keySet();
    resultsByRequest.computeIfAbsent(requestId, r -> new NodeResponseFuture());
    Map<NodeLogicId, List<ResolverCommand>> nodeResults =
        this.resolverResults.computeIfAbsent(requestId, r -> new LinkedHashMap<>());

    Iterable<ResolverDefinition> pendingResolvers;
    if (newInputNames.isEmpty()) {
      pendingResolvers =
          resolverDefinitionsByInput
                  .getOrDefault(Optional.<String>empty(), Collections.emptyList())
                  .stream()
                  .filter(
                      resolverDefinition ->
                          !nodeResults.containsKey(resolverDefinition.resolverNodeLogicId()))
              ::iterator;
    } else {
      pendingResolvers =
          newInputNames.stream()
                  .flatMap(
                      input ->
                          resolverDefinitionsByInput
                              .getOrDefault(Optional.ofNullable(input), ImmutableList.of())
                              .stream()
                              .filter(
                                  resolverDefinition ->
                                      !nodeResults.containsKey(
                                          resolverDefinition.resolverNodeLogicId())))
              ::iterator;
    }
    Map<NodeLogicId, ResolverDefinition> uniquePendingResolvers = new LinkedHashMap<>();
    for (ResolverDefinition pendingResolver : pendingResolvers) {
      uniquePendingResolvers.putIfAbsent(pendingResolver.resolverNodeLogicId(), pendingResolver);
    }
    Map<String, List<ResolverDefinition>> pendingResolverDefinitionsByResolvedDependency =
        new LinkedHashMap<>();
    uniquePendingResolvers.forEach(
        (logicId, resolverDefinition) ->
            pendingResolverDefinitionsByResolvedDependency
                .computeIfAbsent(resolverDefinition.dependencyName(), k -> new ArrayList<>())
                .addAll(
                    resolverDefinitionsByDependency.getOrDefault(
                        resolverDefinition.dependencyName(), ImmutableList.of())));
    int pendingResolverCount = 0;
    for (Entry<String, List<ResolverDefinition>> resolverDefinitions :
        pendingResolverDefinitionsByResolvedDependency.entrySet()) {
      String dependencyName = resolverDefinitions.getKey();
      if (resolverDefinitions.getValue().stream()
          .allMatch(
              resolverDefinition -> allInputNames.containsAll(resolverDefinition.boundFrom()))) {
        pendingResolverCount++;
        List<ResolverCommand> resolverCommands = new ArrayList<>();
        Map<ImmutableSet<String>, ImmutableList<Inputs>> result = new LinkedHashMap<>();
        for (ResolverDefinition resolverDefinition : resolverDefinitions.getValue()) {
          ImmutableSet<String> boundFrom = resolverDefinition.boundFrom();
          NodeLogicId nodeLogicId = resolverDefinition.resolverNodeLogicId();
          Inputs inputs = new Inputs(filterKeys(allInputs.values(), boundFrom::contains));
          ResolverCommand resolverCommand =
              nodeDefinition
                  .nodeDefinitionRegistry()
                  .logicDefinitionRegistry()
                  .getResolver(nodeLogicId)
                  .resolve(inputs);
          resolverCommands.add(resolverCommand);
          nodeResults.put(nodeLogicId, ImmutableList.of(resolverCommand));
          result.put(resolverDefinition.resolvedInputNames(), resolverCommand.getInputs());
        }
        NodeId depNodeId = nodeDefinition.dependencyNodes().get(dependencyName);
        if (resolverCommands.size() > 0
            && resolverCommands.stream()
                .allMatch(resolverCommand -> resolverCommand instanceof SkipDependency)) {
          if (allInputs.values().get(dependencyName) == null) {
            krystalNodeExecutor.enqueueCommand(
                new SkipNode(
                    depNodeId,
                    requestId.append("skip(%s)".formatted(dependencyName)),
                    (SkipDependency) resolverCommands.get(0)));
            this.executeCommand(
                new ExecuteWithDependency(
                    this.nodeId, dependencyName, new Results<>(ImmutableMap.of()), requestId));
          }
        } else {
          List<Inputs> inputPermutations = createInputPermutations(result);
          // Since the node can return multiple results, we have to call the dependency Node
          // multiple times - each with a different request Id.
          int counter = 0;
          Map<Inputs, NodeResponseFuture> nodeResponseFutures = new LinkedHashMap<>();
          for (Inputs resolverInput : inputPermutations) {
            RequestId dependencyRequestId =
                requestId.append("%s[%s]".formatted(dependencyName, counter++));
            NodeResponseFuture nodeResponseFuture =
                krystalNodeExecutor.enqueueCommand(
                    new ExecuteWithAllInputs(depNodeId, resolverInput, dependencyRequestId));
            nodeResponseFutures.put(resolverInput, nodeResponseFuture);
          }
          allOf(
                  nodeResponseFutures.values().stream()
                      .map(NodeResponseFuture::responseFuture)
                      .toArray(CompletableFuture[]::new))
              .whenComplete(
                  (unused, throwable) -> {
                    Map<Inputs, ValueOrError<Object>> results = new LinkedHashMap<>();
                    if (throwable != null) {
                      results.putAll(
                          inputPermutations.stream()
                              .collect(toImmutableMap(identity(), k -> error(throwable))));
                    } else {
                      for (Entry<Inputs, NodeResponseFuture> nodeResponseFuture :
                          nodeResponseFutures.entrySet()) {
                        ValueOrError<Object> now =
                            nodeResponseFuture.getValue().responseFuture().getNow(empty());
                        results.put(nodeResponseFuture.getKey(), now);
                      }
                    }
                    krystalNodeExecutor.enqueueCommand(
                        new ExecuteWithDependency(
                            this.nodeId, dependencyName, new Results<>(results), requestId));
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

  private void executeMainLogic(NodeResponseFuture resultForRequest, RequestId requestId) {
    NodeLogicId mainLogicNode = nodeDefinition.mainLogicNode();
    MainLogicDefinition<Object> mainLogicDefinition =
        nodeDefinition.nodeDefinitionRegistry().logicDefinitionRegistry().getMain(mainLogicNode);
    Map<String, InputValue<Object>> allInputs =
        inputsValueCollector.getOrDefault(requestId, ImmutableMap.of());
    // Retrieve existing result from cache if result for this set of inputs has already been
    // calculated
    CompletableFuture<ValueOrError<Object>> result;
    Inputs nonDependencyInputs =
        new Inputs(
            filterKeys(allInputs, input -> !nodeDefinition.dependencyNodes().containsKey(input)));
    CompletableFuture<ValueOrError<Object>> responseFuture = resultsCache.get(nonDependencyInputs);
    if (responseFuture == null) {
      result =
          executeDecoratedMainLogic(new Inputs(allInputs), mainLogicDefinition)
              .handle(ValueOrError::valueOrError);
      resultsCache.put(nonDependencyInputs, result);
    } else {
      result = responseFuture;
    }

    result.whenComplete(
        (value, throwable) -> {
          if (throwable != null) {
            value = error(throwable);
          }
          resultsCache.computeIfPresent(
              nonDependencyInputs,
              (i, f) -> {
                propagateCompletion(result, f);
                return f;
              });
          resultForRequest.responseFuture().complete(value);
        });
  }

  private CompletableFuture<Object> executeDecoratedMainLogic(
      Inputs inputs, MainLogicDefinition<Object> mainLogicDefinition) {
    Map<String, MainLogicDecorator> decorators =
        new LinkedHashMap<>(mainLogicDefinition.getSessionScopedLogicDecorators());
    // If the same decoratorType is configured for session and request scope, request scope
    // overrides session scope.
    decorators.putAll(requestScopedMainLogicDecorators);
    TreeSet<MainLogicDecorator> sortedDecorators =
        new TreeSet<>(logicDecorationOrdering.decorationOrder());
    sortedDecorators.addAll(decorators.values());
    MainLogic<Object> logic = mainLogicDefinition::execute;
    for (MainLogicDecorator mainLogicDecorator : sortedDecorators) {
      logic = mainLogicDecorator.decorateLogic(logic);
    }
    return logic.execute(ImmutableList.of(inputs)).get(inputs);
  }

  private ImmutableList<Inputs> createInputPermutations(
      Map<ImmutableSet<String>, ImmutableList<Inputs>> inputValues) {
    Set<ImmutableSet<String>> inputNames = inputValues.keySet();
    if (inputNames.isEmpty()) {
      return ImmutableList.of(Inputs.empty());
    }
    ImmutableSet<String> currentInputNames = inputNames.iterator().next();
    ImmutableList<Inputs> currentValues = inputValues.get(currentInputNames);
    if (currentValues == null || currentValues.isEmpty()) {
      // This means no values are present for these inputs.
      // To preserve this information, we need to create an Inputs object with all the keys and
      // empty values corresponding to them
      currentValues =
          ImmutableList.of(
              new Inputs(
                  currentInputNames.stream().collect(toImmutableMap(identity(), k -> empty()))));
    }
    List<Inputs> subPermutation =
        createInputPermutations(filterKeys(inputValues, key -> !currentInputNames.equals(key)));
    if (subPermutation.isEmpty()) {
      return currentValues;
    } else {
      //noinspection UnstableApiUsage
      Builder<Inputs> result =
          ImmutableList.builderWithExpectedSize(currentValues.size() * subPermutation.size());
      for (Inputs currentValue : currentValues) {
        for (Inputs subInputs : subPermutation) {
          Map<String, InputValue<Object>> map = new LinkedHashMap<>();
          map.putAll(currentValue.values());
          map.putAll(subInputs.values());
          result.add(new Inputs(map));
        }
      }
      return result.build();
    }
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

  private static ImmutableMap<String, List<ResolverDefinition>>
      createResolverDefinitionsByDependency(ImmutableList<ResolverDefinition> resolverDefinitions) {
    Map<String, List<ResolverDefinition>> resolverDefinitionsByInput = new LinkedHashMap<>();
    resolverDefinitions.forEach(
        resolverDefinition ->
            resolverDefinitionsByInput
                .computeIfAbsent(resolverDefinition.dependencyName(), s -> new ArrayList<>())
                .add(resolverDefinition));
    return ImmutableMap.copyOf(resolverDefinitionsByInput);
  }
}
