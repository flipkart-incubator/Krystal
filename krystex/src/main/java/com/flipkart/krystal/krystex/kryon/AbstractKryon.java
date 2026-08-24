package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.ContextEnricher;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.decoration.FlushableDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyInvocation;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

@Slf4j
abstract sealed class AbstractKryon<
        C extends KryonCommand<? extends R>, R extends KryonCommandResponse>
    implements Kryon<C, R> permits BatchKryon, DirectKryon {

  protected final VajramKryonDefinition kryonDefinition;
  protected final VajramID vajramID;
  protected final VajramKryonExecutor kryonExecutor;

  private final Function<LogicDecorationContext, List<OutputLogicDecorator>>
      sortedOutputLogicDecoratorsSupplier;

  private final Function<DependencyExecutionContext, List<DependencyDecorator>>
      depDecoratorSuppliers;

  private @MonotonicNonNull List<OutputLogicDecorator> outputLogicDecorators;

  private final Map<Dependency, List<DependencyDecorator>> dependencyDecoratorsByDependency =
      new HashMap<>();

  protected final DecorationOrdering decorationOrdering;

  AbstractKryon(
      VajramKryonDefinition definition,
      VajramKryonExecutor kryonExecutor,
      Function<LogicDecorationContext, List<OutputLogicDecorator>>
          sortedOutputLogicDecoratorsSupplier,
      Function<DependencyExecutionContext, List<DependencyDecorator>> depDecoratorSuppliers,
      DecorationOrdering decorationOrdering) {
    this.kryonDefinition = definition;
    this.vajramID = definition.vajramID();
    this.kryonExecutor = kryonExecutor;
    this.sortedOutputLogicDecoratorsSupplier = sortedOutputLogicDecoratorsSupplier;
    this.depDecoratorSuppliers = depDecoratorSuppliers;
    this.decorationOrdering = decorationOrdering;
  }

  protected List<OutputLogicDecorator> getSortedOutputLogicDecorators() {
    if (outputLogicDecorators == null) {
      outputLogicDecorators =
          sortedOutputLogicDecoratorsSupplier.apply(
              new LogicDecorationContext(
                  vajramID,
                  kryonDefinition.getOutputLogicDefinition().tags(),
                  kryonDefinition.kryonDefinitionRegistry()));
    }
    return outputLogicDecorators;
  }

  protected List<DependencyDecorator> getSortedDependencyDecorators(Dependency dependency) {
    return dependencyDecoratorsByDependency.computeIfAbsent(
        dependency,
        _k -> {
          ImmutableMap<String, Integer> decoratorIndices =
              decorationOrdering.dependencyDecoratorIndices();
          List<DependencyDecorator> radixSortedDecorators =
              new ArrayList<>(decoratorIndices.size());
          decoratorIndices.forEach((_s, _i) -> radixSortedDecorators.add(null));

          List<DependencyDecorator> decoratorsWithNoIndex = new ArrayList<>();
          for (DependencyDecorator decorator :
              depDecoratorSuppliers.apply(new DependencyExecutionContext(dependency))) {
            Integer index = decoratorIndices.get(decorator.decoratorType());
            if (index == null) {
              decoratorsWithNoIndex.add(decorator);
            } else {
              radixSortedDecorators.set(index, decorator);
            }
          }
          List<DependencyDecorator> sortedDecorators =
              new ArrayList<>(radixSortedDecorators.size() + decoratorsWithNoIndex.size());
          sortedDecorators.addAll(decoratorsWithNoIndex);
          for (DependencyDecorator dependencyDecorator : radixSortedDecorators) {
            if (dependencyDecorator != null) {
              sortedDecorators.add(dependencyDecorator);
            }
          }
          return sortedDecorators;
        });
  }

  @Override
  public VajramKryonDefinition getKryonDefinition() {
    return kryonDefinition;
  }

  protected void flushDecorators(DependentChain dependentChain) {
    for (OutputLogicDecorator decorator :
        // Flush in the order in which decorators receive commands
        getSortedOutputLogicDecorators()) {
      if (decorator instanceof FlushableDecorator flushableDecorator) {
        try {
          flushableDecorator.flushDecorator(new FlushCommand(dependentChain));
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
  }

  protected <R2 extends KryonCommandResponse> DependencyInvocation<R2> decorateVajramInvocation(
      Dependency dependency, DependencyInvocation<R2> invocationToDecorate) {
    for (DependencyDecorator dependencyDecorator :
        Lists.reverse(getSortedDependencyDecorators(dependency))) {
      DependencyInvocation<R2> previousDecoratedInvocation = invocationToDecorate;
      invocationToDecorate = dependencyDecorator.decorateDependency(previousDecoratedInvocation);
    }
    return invocationToDecorate;
  }

  ContextEnricher getContextEnricher() {
    return new ContextEnricher() {
      @Override
      public <T, U> BiConsumer<T, U> enrichContext(BiConsumer<T, U> biConsumer) {
        return (t, u) -> {
          VajramID previousActiveVajram = kryonExecutor.executionInfo().activeVajram();
          kryonExecutor.executionInfo().activeVajram(vajramID);
          try {
            biConsumer.accept(t, u);
          } finally {
            kryonExecutor.executionInfo().activeVajram(previousActiveVajram);
          }
        };
      }
    };
  }
}
