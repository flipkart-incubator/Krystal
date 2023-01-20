package com.flipkart.krystal.krystex.node;

import static com.flipkart.krystal.data.ValueOrError.error;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.lang.Math.max;

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
import com.flipkart.krystal.krystex.commands.ExecuteWithInputs;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import com.flipkart.krystal.krystex.commands.SkipNode;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.utils.ImmutableMapView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Node {

  private final NodeId nodeId;

  private final NodeDefinition nodeDefinition;

  private final KrystalNodeExecutor krystalNodeExecutor;

  /** decoratorType -> Decorator */
  private final ImmutableMap<String, MainLogicDecorator> requestScopedMainLogicDecorators;

  private final ImmutableMapView<Optional<String>, List<ResolverDefinition>>
      resolverDefinitionsByInput;
  private final ImmutableMapView<String, List<ResolverDefinition>>
      resolverDefinitionsByDependencies;
  private final LogicDecorationOrdering logicDecorationOrdering;

  private final Map<RequestId, Map<String, DependencyNodeExecutions>> dependencyExecutions =
      new LinkedHashMap<>();

  private final Map<RequestId, Map<ImmutableSet<String>, Inputs>> inputsValueCollector =
      new LinkedHashMap<>();

  private final Map<RequestId, Map<String, Results<Object>>> dependencyValuesCollector =
      new LinkedHashMap<>();

  /** A unique Result future for every requestId. */
  private final Map<RequestId, CompletableFuture<NodeResponse>> resultsByRequest =
      new LinkedHashMap<>();

  private final Map<RequestId, NodeCommand> triggerCommands = new LinkedHashMap<>();

  /**
   * A unique {@link ResultFuture} for every new set of Inputs. This acts as a cache so that the
   * same computation is not repeated multiple times .
   */
  private final Map<Inputs, CompletableFuture<Object>> resultsCache = new LinkedHashMap<>();

  private final Map<RequestId, Map<NodeLogicId, ResolverCommand>> resolverResults =
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
    this.logicDecorationOrdering = logicDecorationOrdering;
    this.resolverDefinitionsByInput =
        createResolverDefinitionsByInputs(nodeDefinition.resolverDefinitions());
    this.resolverDefinitionsByDependencies =
        ImmutableMapView.viewOf(
            nodeDefinition.resolverDefinitions().stream()
                .collect(Collectors.groupingBy(ResolverDefinition::dependencyName)));
  }

  CompletableFuture<NodeResponse> executeCommand(NodeCommand nodeCommand) {
    RequestId requestId = nodeCommand.requestId();
    final CompletableFuture<NodeResponse> resultForRequest =
        resultsByRequest.computeIfAbsent(requestId, r -> new CompletableFuture<>());
    try {
      boolean executeMainLogic;
      if (nodeCommand instanceof SkipNode skipNode) {
        resultForRequest.completeExceptionally(
            new SkipNodeException(skipNode.skipDependencyCommand().reason()));
        return resultForRequest;
      } else if (nodeCommand instanceof ExecuteWithDependency executeWithDependency) {
        executeMainLogic = executeWithDependency(requestId, executeWithDependency);
      } else if (nodeCommand instanceof ExecuteWithAllInputs executeWithAllInputs) {
        triggerCommands.putIfAbsent(requestId, executeWithAllInputs);
        executeMainLogic = executeWithAllInputs(requestId, executeWithAllInputs);
      } else if (nodeCommand instanceof ExecuteWithInputs executeWithInputs) {
        triggerCommands.putIfAbsent(requestId, executeWithInputs);
        executeMainLogic = executeWithInputs(requestId, executeWithInputs);
      } else {
        throw new UnsupportedOperationException(
            "Unknown type of nodeCommand: %s".formatted(nodeCommand));
      }
      if (executeMainLogic) {
        executeMainLogic(resultForRequest, requestId);
      }
    } catch (Exception e) {
      resultForRequest.completeExceptionally(e);
    }
    return resultForRequest;
  }

  private boolean executeWithAllInputs(
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
    return execute(requestId, inputNames);
  }

  private boolean executeWithInputs(RequestId requestId, ExecuteWithInputs executeWithInputs) {
    collectInputValues(requestId, executeWithInputs.inputNames(), executeWithInputs.values());
    return execute(requestId, executeWithInputs.inputNames());
  }

  private boolean executeWithDependency(
      RequestId requestId, ExecuteWithDependency executeWithInput) {
    String dependencyName = executeWithInput.dependencyName();
    ImmutableSet<String> inputNames = ImmutableSet.of(dependencyName);
    if (dependencyValuesCollector
            .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
            .putIfAbsent(dependencyName, executeWithInput.results())
        != null) {
      throw new DuplicateInputForRequestException(
          "Duplicate data for dependency %s for a request %s".formatted(dependencyName, requestId));
    }
    return execute(requestId, inputNames);
  }

  private boolean execute(RequestId requestId, ImmutableSet<String> newInputNames) {
    MainLogicDefinition<Object> mainLogicNodeDefinition =
        nodeDefinition
            .nodeDefinitionRegistry()
            .logicDefinitionRegistry()
            .getMain(nodeDefinition.mainLogicNode());

    Map<ImmutableSet<String>, Inputs> allInputs =
        inputsValueCollector.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    Map<String, Results<Object>> allDependencies =
        dependencyValuesCollector.computeIfAbsent(requestId, k -> new LinkedHashMap<>());
    ImmutableSet<String> allInputNames = mainLogicNodeDefinition.inputNames();
    if (allInputs.keySet().stream().mapToLong(Collection::size).sum() == 0
        && allDependencies.keySet().isEmpty()) {
      if (allInputNames.isEmpty()) {
        return true;
      } else if (nodeDefinition.resolverDefinitions().isEmpty()
          && !nodeDefinition.dependencyNodes().isEmpty())
        return executeDependenciesWhenNoResolvers(requestId);
    }
    Map<NodeLogicId, ResolverCommand> nodeResults =
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
    List<NodeId> dependants = getDependants(requestId);
    int pendingResolverCount = 0;
    for (ResolverDefinition resolverDefinition : uniquePendingResolvers.values()) {
      pendingResolverCount++;
      executeResolver(requestId, dependants, resolverDefinition);
    }

    boolean executeMainLogic = false;
    if (pendingResolverCount == 0) {
      ImmutableSet<String> inputNames = mainLogicNodeDefinition.inputNames();
      Set<String> collect =
          inputsValueCollector.getOrDefault(requestId, ImmutableMap.of()).keySet().stream()
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());
      collect.addAll(dependencyValuesCollector.getOrDefault(requestId, ImmutableMap.of()).keySet());
      if (collect.containsAll(inputNames)) { // All the inputs of the logic node have data present
        executeMainLogic = true;
      }
    }
    return executeMainLogic;
  }

  private void executeResolver(
      RequestId requestId, List<NodeId> dependants, ResolverDefinition resolverDefinition) {
    Map<NodeLogicId, ResolverCommand> nodeResults =
        this.resolverResults.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    String dependencyName = resolverDefinition.dependencyName();
    NodeId depNodeId = nodeDefinition.dependencyNodes().get(dependencyName);
    Inputs inputsForResolver = getInputsForResolver(resolverDefinition, requestId);
    NodeLogicId nodeLogicId = resolverDefinition.resolverNodeLogicId();
    ResolverCommand resolverCommand =
        nodeDefinition
            .nodeDefinitionRegistry()
            .logicDefinitionRegistry()
            .getResolver(nodeLogicId)
            .resolve(inputsForResolver);
    nodeResults.put(nodeLogicId, resolverCommand);
    DependencyNodeExecutions dependencyNodeExecutions =
        dependencyExecutions
            .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
            .computeIfAbsent(dependencyName, k -> new DependencyNodeExecutions());
    dependencyNodeExecutions.executedResolvers().add(resolverDefinition);
    if (resolverCommand instanceof SkipDependency) {
      if (dependencyValuesCollector.getOrDefault(requestId, ImmutableMap.of()).get(dependencyName)
          == null) {
        krystalNodeExecutor.enqueueCommand(
            new SkipNode(
                depNodeId,
                requestId.append("skip(%s)".formatted(dependencyName)),
                (SkipDependency) resolverCommand));
        this.executeCommand(
            new ExecuteWithDependency(
                this.nodeId, dependencyName, new Results<>(ImmutableMap.of()), requestId));
      }
    } else {
      // Since the resolver can return multiple inputs, we have to call the dependency Node
      // multiple times - each with a different request Id.
      // The current resolver  has triggered a fan-out.
      // So we need multiply the total number of requests to the dependency by n where n is
      // the size of the fan-out triggered by this resolver
      ImmutableList<Inputs> inputList = resolverCommand.getInputs();
      long executionsInProgress = dependencyNodeExecutions.executionCounter().longValue();
      int requestCounter = 0;
      for (int j = 0; j < inputList.size(); j++) {
        for (int i = requestCounter; i < requestCounter + max(executionsInProgress, 1); i++) {
          RequestId dependencyRequestId =
              requestId.append("%s[%s]".formatted(dependencyName, requestCounter + i));
          if (requestCounter >= executionsInProgress) {
            dependencyNodeExecutions.executionCounter().increment();
          }
          Inputs inputs = inputList.get(j);
          dependencyNodeExecutions
              .individualCallResponses()
              .putIfAbsent(
                  dependencyRequestId,
                  krystalNodeExecutor.enqueueCommand(
                      new ExecuteWithInputs(
                          depNodeId,
                          resolverDefinition.resolvedInputNames(),
                          inputs,
                          dependencyRequestId,
                          Stream.concat(dependants.stream(), Stream.of(nodeId))
                              .collect(toImmutableList()))));
        }
        requestCounter += max(executionsInProgress, 1);
      }
      if (resolverDefinitionsByDependencies
          .get(dependencyName)
          .equals(dependencyNodeExecutions.executedResolvers)) {
        CompletableFuture.allOf(
                dependencyNodeExecutions
                    .individualCallResponses()
                    .values()
                    .toArray(CompletableFuture[]::new))
            .whenComplete(
                (unused, throwable) -> {
                  Results<Object> results;
                  if (throwable != null) {
                    results = new Results<>(ImmutableMap.of(Inputs.empty(), error(throwable)));
                  } else {
                    results =
                        new Results<>(
                            dependencyNodeExecutions.individualCallResponses().values().stream()
                                .map(cf -> cf.getNow(new NodeResponse()))
                                .collect(
                                    toImmutableMap(NodeResponse::inputs, NodeResponse::response)));
                  }
                  krystalNodeExecutor.enqueueCommand(
                      new ExecuteWithDependency(this.nodeId, dependencyName, results, requestId));
                });
      }
    }
  }

  private Inputs getInputsForResolver(ResolverDefinition resolverDefinition, RequestId requestId) {
    Map<ImmutableSet<String>, Inputs> allInputs =
        inputsValueCollector.computeIfAbsent(requestId, r -> new LinkedHashMap<>());
    ImmutableSet<String> boundFrom = resolverDefinition.boundFrom();
    Map<String, ImmutableSet<String>> inputMappedToInputSet = new LinkedHashMap<>();
    for (ImmutableSet<String> strings : allInputs.keySet()) {
      for (String string : strings) {
        inputMappedToInputSet.put(string, strings);
      }
    }
    Map<String, InputValue<Object>> inputValues = new LinkedHashMap<>();
    for (String boundFromInput : boundFrom) {
      ImmutableSet<String> set = inputMappedToInputSet.get(boundFromInput);
      if (set == null) {
        inputValues.put(
            boundFromInput,
            dependencyValuesCollector
                .computeIfAbsent(requestId, k -> new LinkedHashMap<>())
                .get(boundFromInput));
      } else {
        Inputs inputs = allInputs.get(set);
        inputValues.put(boundFromInput, inputs.values().get(boundFromInput));
      }
    }
    return new Inputs(inputValues);
  }

  private boolean executeDependenciesWhenNoResolvers(RequestId requestId) {
    List<NodeId> dependants = getDependants(requestId);
    nodeDefinition
        .dependencyNodes()
        .forEach(
            (depName, depNodeId) -> {
              if (!dependencyValuesCollector
                  .getOrDefault(requestId, ImmutableMap.of())
                  .containsKey(depName)) {
                RequestId dependencyRequestId = requestId.append("%s".formatted(depName));
                CompletableFuture<NodeResponse> nodeResponse =
                    krystalNodeExecutor.enqueueCommand(
                        new ExecuteWithAllInputs(
                            depNodeId,
                            Inputs.empty(),
                            dependencyRequestId,
                            Stream.concat(dependants.stream(), Stream.of(nodeId))
                                .collect(toImmutableList())));
                nodeResponse
                    .thenApply(NodeResponse::response)
                    .whenComplete(
                        (valueOrError, throwable) -> {
                          if (throwable != null) {
                            valueOrError = error(throwable);
                          }
                          krystalNodeExecutor.enqueueCommand(
                              new ExecuteWithDependency(
                                  this.nodeId,
                                  depName,
                                  new Results<>(ImmutableMap.of(Inputs.empty(), valueOrError)),
                                  requestId));
                        });
              }
            });
    return false;
  }

  private List<NodeId> getDependants(RequestId requestId) {
    return Optional.ofNullable(triggerCommands.getOrDefault(requestId, null))
        .map(NodeCommand::dependants)
        .orElse(ImmutableList.of());
  }

  private void executeMainLogic(
      CompletableFuture<NodeResponse> resultForRequest, RequestId requestId) {
    NodeLogicId mainLogicNode = nodeDefinition.mainLogicNode();
    MainLogicDefinition<Object> mainLogicDefinition =
        nodeDefinition.nodeDefinitionRegistry().logicDefinitionRegistry().getMain(mainLogicNode);
    Map<ImmutableSet<String>, Inputs> allInputs =
        inputsValueCollector.getOrDefault(requestId, ImmutableMap.of());
    // Retrieve existing result from cache if result for this set of inputs has already been
    // calculated
    Map<String, InputValue<Object>> inputsMap = new LinkedHashMap<>();
    allInputs.forEach(
        (strings, inputs) -> strings.forEach(s -> inputsMap.put(s, inputs.values().get(s))));
    Inputs nonDependencyInput = new Inputs(inputsMap);

    CompletableFuture<Object> resultFuture = resultsCache.get(nonDependencyInput);
    if (resultFuture == null) {
      Inputs allInputsAndDependencies =
          new Inputs(
              new LinkedHashMap<>(
                  dependencyValuesCollector.getOrDefault(requestId, ImmutableMap.of())));
      allInputsAndDependencies.mergeFrom(nonDependencyInput);
      resultFuture =
          executeDecoratedMainLogic(allInputsAndDependencies, mainLogicDefinition, requestId);
      resultsCache.put(nonDependencyInput, resultFuture);
    }
    resultFuture
        .handle(ValueOrError::valueOrError)
        .thenAccept(
            value -> resultForRequest.complete(new NodeResponse(nonDependencyInput, value)));
  }

  private void collectInputValues(
      RequestId requestId, ImmutableSet<String> inputNames, Inputs inputs) {
    if (inputsValueCollector
            .computeIfAbsent(requestId, r -> new LinkedHashMap<>())
            .putIfAbsent(inputNames, inputs)
        != null) {
      throw new DuplicateInputForRequestException(
          "Duplicate input data for a request %s".formatted(requestId));
    }
  }

  private CompletableFuture<Object> executeDecoratedMainLogic(
      Inputs inputs, MainLogicDefinition<Object> mainLogicDefinition, RequestId requestId) {
    Map<String, MainLogicDecorator> decorators =
        new LinkedHashMap<>(
            mainLogicDefinition.getSessionScopedLogicDecorators(
                nodeDefinition, getDependants(requestId)));
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

  private static ImmutableMapView<Optional<String>, List<ResolverDefinition>>
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
    return ImmutableMapView.viewOf(resolverDefinitionsByInput);
  }

  private record DependencyNodeExecutions(
      LongAdder executionCounter,
      List<ResolverDefinition> executedResolvers,
      Map<RequestId, CompletableFuture<NodeResponse>> individualCallResponses) {

    public DependencyNodeExecutions() {
      this(new LongAdder(), new ArrayList<>(), new LinkedHashMap<>());
    }
  }
}
