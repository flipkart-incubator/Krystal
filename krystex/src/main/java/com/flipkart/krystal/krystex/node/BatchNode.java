package com.flipkart.krystal.krystex.node;

import static com.flipkart.krystal.krystex.node.NodeUtils.createResolverDefinitionsByInputs;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.Results;
import com.flipkart.krystal.krystex.commands.BatchNodeCommand;
import com.flipkart.krystal.krystex.commands.CallbackGranularCommand;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardGranularCommand;
import com.flipkart.krystal.krystex.commands.SkipGranularCommand;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutor.ResolverExecStrategy;
import com.flipkart.krystal.krystex.request.RequestId;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.flipkart.krystal.utils.ImmutableMapView;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

final class BatchNode implements Node<BatchNodeCommand, BatchNodeResponse> {

  private final NodeId nodeId;

  private final NodeDefinition nodeDefinition;

  private final KrystalNodeExecutor krystalNodeExecutor;

  /** decoratorType -> Decorator */
  private final Function<LogicExecutionContext, ImmutableMap<String, MainLogicDecorator>>
      requestScopedDecoratorsSupplier;

  private final ImmutableMapView<Optional<String>, List<ResolverDefinition>>
      resolverDefinitionsByInput;
  private final ImmutableMapView<String, ImmutableSet<ResolverDefinition>>
      resolverDefinitionsByDependencies;
  private final ResolverExecStrategy resolverExecStrategy;
  private final LogicDecorationOrdering logicDecorationOrdering;

  private final Map<DependantChain, Map<String, DependencyNodeExecutions>> dependencyExecutions =
      new LinkedHashMap<>();

  private final Map<RequestId, Map<String, InputValue<Object>>> inputsValueCollector =
      new LinkedHashMap<>();

  private final Map<RequestId, Map<String, Results<Object>>> dependencyValuesCollector =
      new LinkedHashMap<>();

  /** A unique Result future for every requestId. */
  private final Map<DependantChain, CompletableFuture<BatchNodeResponse>> resultsByDepChain =
      new LinkedHashMap<>();

  /**
   * A unique {@link CompletableFuture} for every new set of Inputs. This acts as a cache so that
   * the same computation is not repeated multiple times .
   */
  private final Map<Inputs, CompletableFuture<Object>> resultsCache = new LinkedHashMap<>();

  private final Map<DependantChain, Boolean> mainLogicExecuted = new LinkedHashMap<>();

  private final Map<DependantChain, Optional<String>> skipLogicRequested = new LinkedHashMap<>();

  private final Map<DependantChain, Set<ResolverDefinition>> executedResults =
      new LinkedHashMap<>();

  private final Map<DependantChain, Boolean> flushedDependantChain = new LinkedHashMap<>();

  BatchNode(
      NodeDefinition nodeDefinition,
      KrystalNodeExecutor krystalNodeExecutor,
      Function<LogicExecutionContext, ImmutableMap<String, MainLogicDecorator>>
          requestScopedDecoratorsSupplier,
      LogicDecorationOrdering logicDecorationOrdering,
      ResolverExecStrategy resolverExecStrategy) {
    this.nodeId = nodeDefinition.nodeId();
    this.nodeDefinition = nodeDefinition;
    this.krystalNodeExecutor = krystalNodeExecutor;
    this.requestScopedDecoratorsSupplier = requestScopedDecoratorsSupplier;
    this.logicDecorationOrdering = logicDecorationOrdering;
    this.resolverDefinitionsByInput =
        createResolverDefinitionsByInputs(nodeDefinition.resolverDefinitions());
    this.resolverDefinitionsByDependencies =
        ImmutableMapView.viewOf(
            nodeDefinition.resolverDefinitions().stream()
                .collect(groupingBy(ResolverDefinition::dependencyName, toImmutableSet())));
    this.resolverExecStrategy = resolverExecStrategy;
  }

  @Override
  public void executeCommand(Flush nodeCommand) {}

  @Override
  public CompletableFuture<BatchNodeResponse> executeCommand(BatchNodeCommand nodeCommand) {
    DependantChain dependantChain = nodeCommand.dependantChain();
    final CompletableFuture<BatchNodeResponse> resultForDepChain =
        resultsByDepChain.computeIfAbsent(dependantChain, r -> new CompletableFuture<>());
    if (resultForDepChain.isDone()) {
      // This is possible if this node was already skipped, for example.
      // If the result for this requestId is already available, just return and avoid unnecessary
      // computation.
      return resultForDepChain;
    }
//    try {
//      if (nodeCommand instanceof SkipGranularCommand skipNode) {
//        requestsByDependantChain
//            .computeIfAbsent(skipNode.dependantChain(), k -> new LinkedHashSet<>())
//            .add(requestId);
//        dependantChainByRequest.put(requestId, skipNode.dependantChain());
//        skipLogicRequested.put(requestId, Optional.of(skipNode));
//        return handleSkipDependency(requestId, skipNode, resultForDepChain);
//      } else if (nodeCommand instanceof CallbackGranularCommand callbackGranularCommand) {
//        executeWithDependency(requestId, callbackGranularCommand);
//      } else if (nodeCommand instanceof ForwardGranularCommand forwardGranularCommand) {
//        requestsByDependantChain
//            .computeIfAbsent(forwardGranularCommand.dependantChain(), k -> new LinkedHashSet<>())
//            .add(requestId);
//        dependantChainByRequest.computeIfAbsent(
//            requestId, r -> forwardGranularCommand.dependantChain());
//        executeWithInputs(requestId, forwardGranularCommand);
//      } else {
//        throw new UnsupportedOperationException(
//            "Unknown type of nodeCommand: %s".formatted(nodeCommand));
//      }
//      executeMainLogicIfPossible(requestId, resultForDepChain);
//    } catch (Throwable e) {
//      resultForDepChain.completeExceptionally(e);
//    }
    return resultForDepChain;
  }

  private record DependencyNodeExecutions(
      LongAdder executionCounter,
      Set<ResolverDefinition> executedResolvers,
      Map<DependantChain, Inputs> individualCallInputs,
      Map<DependantChain, CompletableFuture<GranularNodeResponse>> individualCallResponses) {

    private DependencyNodeExecutions() {
      this(new LongAdder(), new LinkedHashSet<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }
  }
}
