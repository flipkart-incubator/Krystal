package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.ContextEnricher;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.decoration.FlushableDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyInvocation;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
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

@Slf4j
abstract sealed class AbstractKryon<
        C extends KryonCommand<? extends R>, R extends KryonCommandResponse>
    implements Kryon<C, R> permits BatchKryon, DirectKryon {
  /**
   * Initial capacity for maps and sets. In load tests in real-world applications, substantial CPU
   * was observed to be spent in resizing collections.
   */
  static final int INITIAL_CAPACITY = 64;

  protected final VajramKryonDefinition kryonDefinition;
  protected final VajramID vajramID;
  protected final VajramKryonExecutor kryonExecutor;

  /** decoratorType -> Decorator */
  protected final Function<LogicExecutionContext, List<OutputLogicDecorator>>
      sortedOutputLogicDecoratorsSupplier;

  private final Function<DependencyExecutionContext, ImmutableMap<String, DependencyDecorator>>
      depDecoratorSuppliers;

  private final Map<DependentChain, List<OutputLogicDecorator>> outputLogicDecoratorsByDepChain =
      new HashMap<>(INITIAL_CAPACITY);

  private final Map<VajramID, List<DependencyDecorator>> dependencyDecoratorsByDepVajram =
      new HashMap<>();

  protected final DecorationOrdering decorationOrdering;

  AbstractKryon(
      VajramKryonDefinition definition,
      VajramKryonExecutor kryonExecutor,
      Function<LogicExecutionContext, List<OutputLogicDecorator>>
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

  protected List<OutputLogicDecorator> getSortedOutputLogicDecorators(
      DependentChain dependentChain) {
    OutputLogicDefinition<Object> outputLogicDefinition =
        kryonDefinition.getOutputLogicDefinition();
    return outputLogicDecoratorsByDepChain.computeIfAbsent(
        dependentChain,
        _d ->
            sortedOutputLogicDecoratorsSupplier.apply(
                new LogicExecutionContext(
                    vajramID,
                    outputLogicDefinition.tags(),
                    dependentChain,
                    kryonDefinition.kryonDefinitionRegistry())));
  }

  protected List<DependencyDecorator> getSortedDependencyDecorators(
      VajramID depVajramId, DependentChain dependentChain) {
    return dependencyDecoratorsByDepVajram.computeIfAbsent(
        depVajramId,
        _k -> {
          ImmutableMap<String, Integer> decoratorIndices =
              decorationOrdering.dependencyDecoratorIndices();
          List<DependencyDecorator> radixSortedDecorators =
              new ArrayList<>(decoratorIndices.size());
          decoratorIndices.forEach((_s, _i) -> radixSortedDecorators.add(null));

          List<DependencyDecorator> decoratorsWithNoIndex = new ArrayList<>();
          for (DependencyDecorator decorator :
              depDecoratorSuppliers
                  .apply(new DependencyExecutionContext(depVajramId, dependentChain))
                  .values()) {
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
        getSortedOutputLogicDecorators(dependentChain)) {
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
      DependentChain dependentChain,
      VajramID depVajramID,
      DependencyInvocation<R2> invocationToDecorate) {
    for (DependencyDecorator dependencyDecorator :
        Lists.reverse(getSortedDependencyDecorators(depVajramID, dependentChain))) {
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
