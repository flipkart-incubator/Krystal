package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.KryonDefinition.KryonDefinitionView;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;

abstract sealed class AbstractKryon<C extends KryonCommand, R extends KryonResponse>
    implements Kryon<C, R> permits BatchKryon, GranularKryon {

  protected final KryonDefinition definition;
  protected final KryonId kryonId;
  protected final KryonExecutor kryonExecutor;

  /** decoratorType -> Decorator */
  protected final Function<LogicExecutionContext, ImmutableMap<String, MainLogicDecorator>>
      requestScopedDecoratorsSupplier;

  protected final LogicDecorationOrdering logicDecorationOrdering;

  protected final ImmutableMap<Optional<String>, ImmutableSet<ResolverDefinition>>
      resolverDefinitionsByInput;
  protected final ImmutableMap<String, ImmutableSet<ResolverDefinition>>
      resolverDefinitionsByDependencies;
  protected final ImmutableSet<String> dependenciesWithNoResolvers;
  protected final RequestIdGenerator requestIdGenerator;

  AbstractKryon(
      KryonDefinition definition,
      KryonExecutor kryonExecutor,
      Function<LogicExecutionContext, ImmutableMap<String, MainLogicDecorator>>
          requestScopedDecoratorsSupplier,
      LogicDecorationOrdering logicDecorationOrdering,
      RequestIdGenerator requestIdGenerator) {
    this.definition = definition;
    this.kryonId = definition.kryonId();
    this.kryonExecutor = kryonExecutor;
    this.requestScopedDecoratorsSupplier = requestScopedDecoratorsSupplier;
    this.logicDecorationOrdering = logicDecorationOrdering;
    KryonDefinitionView kryonDefinitionView = definition.kryonDefinitionView();
    this.resolverDefinitionsByInput = kryonDefinitionView.resolverDefinitionsByInput();
    this.resolverDefinitionsByDependencies =
        kryonDefinitionView.resolverDefinitionsByDependencies();
    this.dependenciesWithNoResolvers = kryonDefinitionView.dependenciesWithNoResolvers();
    this.requestIdGenerator = requestIdGenerator;
  }

  protected NavigableSet<MainLogicDecorator> getSortedDecorators(DependantChain dependantChain) {
    MainLogicDefinition<Object> mainLogicDefinition = definition.getMainLogicDefinition();
    Map<String, MainLogicDecorator> decorators =
        new LinkedHashMap<>(
            mainLogicDefinition.getSessionScopedLogicDecorators(definition, dependantChain));
    // If the same decoratorType is configured for session and request scope, request scope
    // overrides session scope.
    decorators.putAll(
        requestScopedDecoratorsSupplier.apply(
            new LogicExecutionContext(
                kryonId,
                mainLogicDefinition.logicTags(),
                dependantChain,
                definition.kryonDefinitionRegistry())));
    TreeSet<MainLogicDecorator> sortedDecorators =
        new TreeSet<>(logicDecorationOrdering.decorationOrder());
    sortedDecorators.addAll(decorators.values());
    return sortedDecorators;
  }

  @Override
  public KryonDefinition getDefinition() {
    return definition;
  }
}
