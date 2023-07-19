package com.flipkart.krystal.krystex.node;

import static com.flipkart.krystal.krystex.node.NodeUtils.createResolverDefinitionsByInputs;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;

import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.commands.NodeDataCommand;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutor.ResolverExecStrategy;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.flipkart.krystal.utils.ImmutableMapView;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;

abstract sealed class AbstractNode<C extends NodeDataCommand, R extends NodeResponse>
    implements Node<C, R> permits BatchNode, GranularNode {

  protected final NodeDefinition nodeDefinition;
  protected final NodeId nodeId;
  protected final KrystalNodeExecutor krystalNodeExecutor;
  /** decoratorType -> Decorator */
  protected final Function<LogicExecutionContext, ImmutableMap<String, MainLogicDecorator>>
      requestScopedDecoratorsSupplier;

  protected final LogicDecorationOrdering logicDecorationOrdering;
  protected final ResolverExecStrategy resolverExecStrategy;

  protected final ImmutableMapView<Optional<String>, List<ResolverDefinition>>
      resolverDefinitionsByInput;
  protected final ImmutableMapView<String, ImmutableSet<ResolverDefinition>>
      resolverDefinitionsByDependencies;
  protected final ImmutableSet<String> dependenciesWithNoResolvers;
  protected final RequestIdGenerator requestIdGenerator;

  AbstractNode(
      NodeDefinition nodeDefinition,
      KrystalNodeExecutor krystalNodeExecutor,
      Function<LogicExecutionContext, ImmutableMap<String, MainLogicDecorator>>
          requestScopedDecoratorsSupplier,
      LogicDecorationOrdering logicDecorationOrdering,
      ResolverExecStrategy resolverExecStrategy,
      RequestIdGenerator requestIdGenerator) {
    this.nodeDefinition = nodeDefinition;
    this.nodeId = nodeDefinition.nodeId();
    this.krystalNodeExecutor = krystalNodeExecutor;
    this.requestScopedDecoratorsSupplier = requestScopedDecoratorsSupplier;
    this.logicDecorationOrdering = logicDecorationOrdering;
    this.resolverExecStrategy = resolverExecStrategy;
    this.resolverDefinitionsByInput =
        createResolverDefinitionsByInputs(nodeDefinition.resolverDefinitions());
    this.resolverDefinitionsByDependencies =
        ImmutableMapView.viewOf(
            nodeDefinition.resolverDefinitions().stream()
                .collect(groupingBy(ResolverDefinition::dependencyName, toImmutableSet())));
    this.dependenciesWithNoResolvers =
        nodeDefinition.dependencyNodes().keySet().stream()
            .filter(
                depName ->
                    resolverDefinitionsByDependencies
                        .getOrDefault(depName, ImmutableSet.of())
                        .isEmpty())
            .collect(toImmutableSet());
    this.requestIdGenerator = requestIdGenerator;
  }

  protected NavigableSet<MainLogicDecorator> getSortedDecorators(DependantChain dependantChain) {
    MainLogicDefinition<Object> mainLogicDefinition = nodeDefinition.getMainLogicDefinition();
    Map<String, MainLogicDecorator> decorators =
        new LinkedHashMap<>(
            mainLogicDefinition.getSessionScopedLogicDecorators(nodeDefinition, dependantChain));
    // If the same decoratorType is configured for session and request scope, request scope
    // overrides session scope.
    decorators.putAll(
        requestScopedDecoratorsSupplier.apply(
            new LogicExecutionContext(
                nodeId,
                mainLogicDefinition.logicTags(),
                dependantChain,
                nodeDefinition.nodeDefinitionRegistry())));
    TreeSet<MainLogicDecorator> sortedDecorators =
        new TreeSet<>(logicDecorationOrdering.decorationOrder());
    sortedDecorators.addAll(decorators.values());
    return sortedDecorators;
  }
}
