package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Function;

abstract sealed class AbstractKryon<C extends KryonCommand, R extends KryonCommandResponse>
    implements Kryon<C, R> permits FlushableKryon {

  protected final VajramKryonDefinition kryonDefinition;
  protected final VajramID vajramID;
  protected final KryonExecutor kryonExecutor;

  /** decoratorType -> Decorator */
  protected final Function<LogicExecutionContext, Map<String, OutputLogicDecorator>>
      outputLogicDecoratorSuppliers;

  private final Function<DependencyExecutionContext, ImmutableMap<String, DependencyDecorator>>
      depDecoratorSuppliers;

  private final Map<DependentChain, NavigableSet<OutputLogicDecorator>>
      requestScopedDecoratorsByDepChain = new HashMap<>();

  protected final DecorationOrdering decorationOrdering;

  protected final RequestIdGenerator requestIdGenerator;

  AbstractKryon(
      VajramKryonDefinition definition,
      KryonExecutor kryonExecutor,
      Function<LogicExecutionContext, Map<String, OutputLogicDecorator>>
          outputLogicDecoratorSuppliers,
      Function<DependencyExecutionContext, ImmutableMap<String, DependencyDecorator>>
          depDecoratorSuppliers,
      DecorationOrdering decorationOrdering,
      RequestIdGenerator requestIdGenerator) {
    this.kryonDefinition = definition;
    this.vajramID = definition.vajramID();
    this.kryonExecutor = kryonExecutor;
    this.outputLogicDecoratorSuppliers = outputLogicDecoratorSuppliers;
    this.depDecoratorSuppliers = depDecoratorSuppliers;
    this.decorationOrdering = decorationOrdering;
    this.requestIdGenerator = requestIdGenerator;
  }

  protected NavigableSet<OutputLogicDecorator> getSortedOutputLogicDecorators(
      DependentChain dependantChain) {
    return requestScopedDecoratorsByDepChain.computeIfAbsent(
        dependantChain,
        _d -> {
          TreeSet<OutputLogicDecorator> sortedDecorators =
              new TreeSet<>(
                  decorationOrdering
                      .encounterOrder()
                      // Reverse the ordering so that the ones with the highest index are applied
                      // first.
                      .reversed());
          OutputLogicDefinition<Object> outputLogicDefinition =
              kryonDefinition.getOutputLogicDefinition();
          // If the same decoratorType is configured for session and request scope, it will get
          // applied twice. This is not ideal and will be fixed in future Krystal versions
          sortedDecorators.addAll(
              outputLogicDecoratorSuppliers
                  .apply(
                      new LogicExecutionContext(
                          vajramID,
                          outputLogicDefinition.tags(),
                          dependantChain,
                          kryonDefinition.kryonDefinitionRegistry()))
                  .values());
          return sortedDecorators;
        });
  }

  protected NavigableSet<DependencyDecorator> getSortedDependencyDecorators(
      VajramID depVajramId, DependentChain dependentChain) {
    TreeSet<DependencyDecorator> sortedDecorators =
        new TreeSet<>(
            decorationOrdering
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
