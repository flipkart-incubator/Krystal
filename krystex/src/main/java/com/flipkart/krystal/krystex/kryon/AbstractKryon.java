package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Function;

abstract sealed class AbstractKryon<C extends KryonCommand, R extends KryonCommandResponse>
    implements Kryon<C, R> permits FlushableKryon, StreamingKryon {

  protected final VajramKryonDefinition kryonDefinition;
  protected final VajramID vajramID;
  protected final KryonExecutor kryonExecutor;

  /** decoratorType -> Decorator */
  protected final Function<LogicExecutionContext, ImmutableMap<String, OutputLogicDecorator>>
      outputLogicDecoratorSuppliers;

  private final Function<DependencyExecutionContext, ImmutableMap<String, DependencyDecorator>>
      depDecoratorSuppliers;
  protected final LogicDecorationOrdering logicDecorationOrdering;

  protected final RequestIdGenerator requestIdGenerator;

  AbstractKryon(
      VajramKryonDefinition definition,
      KryonExecutor kryonExecutor,
      Function<LogicExecutionContext, ImmutableMap<String, OutputLogicDecorator>>
          outputLogicDecoratorSuppliers,
      Function<DependencyExecutionContext, ImmutableMap<String, DependencyDecorator>>
          depDecoratorSuppliers,
      LogicDecorationOrdering logicDecorationOrdering,
      RequestIdGenerator requestIdGenerator) {
    this.kryonDefinition = definition;
    this.vajramID = definition.vajramID();
    this.kryonExecutor = kryonExecutor;
    this.outputLogicDecoratorSuppliers = outputLogicDecoratorSuppliers;
    this.depDecoratorSuppliers = depDecoratorSuppliers;
    this.logicDecorationOrdering = logicDecorationOrdering;
    this.requestIdGenerator = requestIdGenerator;
  }

  protected NavigableSet<OutputLogicDecorator> getSortedOutputLogicDecorators(
      DependentChain dependentChain) {
    OutputLogicDefinition<Object> outputLogicDefinition =
        kryonDefinition.getOutputLogicDefinition();
    Map<String, OutputLogicDecorator> decorators =
        new LinkedHashMap<>(
            outputLogicDefinition.getSessionScopedLogicDecorators(kryonDefinition, dependentChain));
    // If the same decoratorType is configured for session and request scope, request scope
    // overrides session scope.
    decorators.putAll(
        outputLogicDecoratorSuppliers.apply(
            new LogicExecutionContext(
                vajramID,
                outputLogicDefinition.tags(),
                dependentChain,
                kryonDefinition.kryonDefinitionRegistry())));
    TreeSet<OutputLogicDecorator> sortedDecorators =
        new TreeSet<>(
            logicDecorationOrdering
                .encounterOrder()
                // Reverse the ordering so that the ones with the highest index are applied first.
                .reversed());
    sortedDecorators.addAll(decorators.values());
    return sortedDecorators;
  }

  protected NavigableSet<DependencyDecorator> getSortedDependencyDecorators(
      VajramID depVajramId, DependentChain dependentChain) {
    // If the same decoratorType is configured for session and request scope, request scope
    // overrides session scope.
    TreeSet<DependencyDecorator> sortedDecorators =
        new TreeSet<>(
            logicDecorationOrdering
                .encounterOrder()
                // Reverse the ordering so that the ones with the highest index are applied first.
                .reversed());
    Dependency dependency = dependentChain.latestDependency();
    if (dependency == null) {
      return sortedDecorators;
    }
    sortedDecorators.addAll(
        depDecoratorSuppliers
            .apply(
                new DependencyExecutionContext(vajramID, dependency, depVajramId, dependentChain))
            .values());
    return sortedDecorators;
  }

  @Override
  public VajramKryonDefinition getKryonDefinition() {
    return kryonDefinition;
  }
}
