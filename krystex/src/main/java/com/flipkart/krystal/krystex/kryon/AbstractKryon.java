package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Function;

abstract sealed class AbstractKryon<C extends KryonCommand, R extends KryonResponse>
    implements Kryon<C, R> permits BatchKryon, GranularKryon {

  protected final KryonDefinition kryonDefinition;
  protected final KryonId kryonId;
  protected final KryonExecutor kryonExecutor;

  /** decoratorType -> Decorator */
  protected final Function<LogicExecutionContext, Map<String, OutputLogicDecorator>>
      requestScopedDecoratorsSupplier;

  private final Map<DependantChain, NavigableSet<OutputLogicDecorator>>
      requestScopedDecoratorsByDepChain = new HashMap<>();

  protected final LogicDecorationOrdering logicDecorationOrdering;

  protected final RequestIdGenerator requestIdGenerator;

  AbstractKryon(
      KryonDefinition definition,
      KryonExecutor kryonExecutor,
      Function<LogicExecutionContext, Map<String, OutputLogicDecorator>>
          requestScopedDecoratorsSupplier,
      LogicDecorationOrdering logicDecorationOrdering,
      RequestIdGenerator requestIdGenerator) {
    this.kryonDefinition = definition;
    this.kryonId = definition.kryonId();
    this.kryonExecutor = kryonExecutor;
    this.requestScopedDecoratorsSupplier = requestScopedDecoratorsSupplier;
    this.logicDecorationOrdering = logicDecorationOrdering;
    this.requestIdGenerator = requestIdGenerator;
  }

  protected NavigableSet<OutputLogicDecorator> getSortedDecorators(DependantChain dependantChain) {
    return requestScopedDecoratorsByDepChain.computeIfAbsent(
        dependantChain,
        _d -> {
          TreeSet<OutputLogicDecorator> sortedDecorators =
              new TreeSet<>(
                  logicDecorationOrdering
                      .encounterOrder()
                      // Reverse the ordering so that the ones with the highest index are applied
                      // first.
                      .reversed());
          OutputLogicDefinition<Object> outputLogicDefinition =
              kryonDefinition.getOutputLogicDefinition();
          sortedDecorators.addAll(
              outputLogicDefinition
                  .getSessionScopedLogicDecorators(kryonDefinition, dependantChain)
                  .values());
          // If the same decoratorType is configured for session and request scope, it will get
          // applied twice. This is not ideal and will be fixed in future Krystal versions
          sortedDecorators.addAll(
              requestScopedDecoratorsSupplier
                  .apply(
                      new LogicExecutionContext(
                          kryonId,
                          outputLogicDefinition.tags(),
                          dependantChain,
                          kryonDefinition.kryonDefinitionRegistry()))
                  .values());
          return sortedDecorators;
        });
  }

  @Override
  public KryonDefinition getKryonDefinition() {
    return kryonDefinition;
  }
}
