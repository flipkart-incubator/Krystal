package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyInvocation;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract sealed class AbstractKryon<C extends KryonCommand<?>, R extends KryonCommandResponse>
    implements Kryon<C, R> permits BatchKryon, DirectKryon {
  /**
   * Initial capacity for maps and sets. In load tests in real-world applications, substantial CPU
   * was observed to be spent in resizing collections.
   */
  static final int INITIAL_CAPACITY = 64;

  protected final VajramKryonDefinition kryonDefinition;
  protected final VajramID vajramID;
  protected final KryonExecutor kryonExecutor;

  /** decoratorType -> Decorator */
  protected final Function<LogicExecutionContext, NavigableSet<OutputLogicDecorator>>
      sortedOutputLogicDecoratorsSupplier;

  private final Function<DependencyExecutionContext, ImmutableMap<String, DependencyDecorator>>
      depDecoratorSuppliers;

  private final Map<DependentChain, NavigableSet<OutputLogicDecorator>>
      requestScopedDecoratorsByDepChain = new HashMap<>(INITIAL_CAPACITY);

  protected final DecorationOrdering decorationOrdering;

  private final Map<VajramID, NavigableSet<DependencyDecorator>> decoratorsByDependency =
      new HashMap<>();

  AbstractKryon(
      VajramKryonDefinition definition,
      KryonExecutor kryonExecutor,
      Function<LogicExecutionContext, NavigableSet<OutputLogicDecorator>>
          sortedOutputLogicDecoratorsSupplier,
      Function<DependencyExecutionContext, ImmutableMap<String, DependencyDecorator>>
          depDecoratorSuppliers,
      DecorationOrdering decorationOrdering) {
    this.kryonDefinition = definition;
    this.vajramID = definition.vajramID();
    this.kryonExecutor = kryonExecutor;
    this.sortedOutputLogicDecoratorsSupplier = sortedOutputLogicDecoratorsSupplier;
    this.depDecoratorSuppliers = depDecoratorSuppliers;
    this.decorationOrdering = decorationOrdering;
  }

  protected NavigableSet<OutputLogicDecorator> getSortedOutputLogicDecorators(
      DependentChain dependentChain) {
    OutputLogicDefinition<Object> outputLogicDefinition =
        kryonDefinition.getOutputLogicDefinition();
    return requestScopedDecoratorsByDepChain.computeIfAbsent(
        dependentChain,
        _d ->
            sortedOutputLogicDecoratorsSupplier.apply(
                new LogicExecutionContext(
                    vajramID,
                    outputLogicDefinition.tags(),
                    dependentChain,
                    kryonDefinition.kryonDefinitionRegistry())));
  }

  protected NavigableSet<DependencyDecorator> getSortedDependencyDecorators(
      VajramID depVajramId, DependentChain dependentChain) {
    return decoratorsByDependency.computeIfAbsent(
        depVajramId,
        _k -> {
          TreeSet<DependencyDecorator> sortedDecorators =
              new TreeSet<>(
                  decorationOrdering
                      .encounterOrder()
                      // Reverse the ordering so that the ones with the highest index are applied
                      // first.
                      .reversed());
          Dependency dependency = dependentChain.latestDependency();
          if (dependency == null) {
            return sortedDecorators;
          }
          sortedDecorators.addAll(
              depDecoratorSuppliers
                  .apply(new DependencyExecutionContext(dependency, dependentChain))
                  .values());
          return sortedDecorators;
        });
  }

  @Override
  public VajramKryonDefinition getKryonDefinition() {
    return kryonDefinition;
  }

  protected void flushDecorators(DependentChain dependentChain) {
    Iterable<OutputLogicDecorator> reverseSortedDecorators =
        getSortedOutputLogicDecorators(dependentChain)::descendingIterator;
    for (OutputLogicDecorator decorator : reverseSortedDecorators) {
      try {
        decorator.executeCommand(new FlushCommand(dependentChain));
      } catch (Throwable e) {
        log.error(
            """
                Error while flushing decorator: {}. \
                This is most probably a bug since decorator methods are not supposed to throw exceptions. \
                This can cause unpredictable behaviour in the krystal graph execution. \
                Please fix!""",
            decorator,
            e);
      }
    }
  }

  protected <R2 extends KryonCommandResponse> DependencyInvocation<R2> decorateVajramInvocation(
      DependentChain dependentChain,
      VajramID depVajramID,
      DependencyInvocation<R2> invocationToDecorate) {
    for (DependencyDecorator dependencyDecorator :
        getSortedDependencyDecorators(depVajramID, dependentChain)) {
      DependencyInvocation<R2> previousDecoratedInvocation = invocationToDecorate;
      invocationToDecorate = dependencyDecorator.decorateDependency(previousDecoratedInvocation);
    }
    return invocationToDecorate;
  }
}
