package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Function;

abstract sealed class AbstractKryon<C extends KryonCommand, R extends KryonCommandResponse>
    implements Kryon<C, R> permits BatchKryon {
  /**
   * Initial capacity for maps and sets. In load tests in real-world applications, substantial CPU
   * was observed to be spent in resizing collections.
   */
  static final int INITIAL_CAPACITY = 64;

  protected final VajramKryonDefinition kryonDefinition;
  protected final VajramID vajramID;
  protected final KryonExecutor kryonExecutor;

  /** decoratorType -> Decorator */
  protected final Function<LogicExecutionContext, Collection<OutputLogicDecorator>>
      outputLogicDecoratorsSupplier;

  private final Function<DependencyExecutionContext, ImmutableMap<String, DependencyDecorator>>
      depDecoratorSuppliers;

  private final Map<DependentChain, NavigableSet<OutputLogicDecorator>>
      requestScopedDecoratorsByDepChain = new HashMap<>(INITIAL_CAPACITY);

  protected final DecorationOrdering decorationOrdering;

  protected final RequestIdGenerator requestIdGenerator;

  AbstractKryon(
      VajramKryonDefinition definition,
      KryonExecutor kryonExecutor,
      Function<LogicExecutionContext, Collection<OutputLogicDecorator>>
          outputLogicDecoratorsSupplier,
      Function<DependencyExecutionContext, ImmutableMap<String, DependencyDecorator>>
          depDecoratorSuppliers,
      DecorationOrdering decorationOrdering,
      RequestIdGenerator requestIdGenerator) {
    this.kryonDefinition = definition;
    this.vajramID = definition.vajramID();
    this.kryonExecutor = kryonExecutor;
    this.outputLogicDecoratorsSupplier = outputLogicDecoratorsSupplier;
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
          sortedDecorators.addAll(
              outputLogicDecoratorsSupplier.apply(
                  new LogicExecutionContext(
                      vajramID,
                      kryonDefinition.getOutputLogicDefinition().tags(),
                      dependantChain,
                      kryonDefinition.kryonDefinitionRegistry())));
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
