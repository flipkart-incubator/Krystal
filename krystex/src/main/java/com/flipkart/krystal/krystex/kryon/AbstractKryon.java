package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.concurrent.Futures;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.ClientSideCommand;
import com.flipkart.krystal.krystex.commands.DirectForwardSend;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyInvocation;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

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
          sortedDecorators.addAll(
              depDecoratorSuppliers
                  .apply(new DependencyExecutionContext(depVajramId, dependentChain))
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
    invocationToDecorate = decorateWithExecutionInfoUpdater(invocationToDecorate);
    for (DependencyDecorator dependencyDecorator :
        getSortedDependencyDecorators(depVajramID, dependentChain)) {
      DependencyInvocation<R2> previousDecoratedInvocation = invocationToDecorate;
      invocationToDecorate = dependencyDecorator.decorateDependency(previousDecoratedInvocation);
    }
    return invocationToDecorate;
  }

  @SuppressWarnings("unchecked")
  private <R2 extends KryonCommandResponse>
      DependencyInvocation<R2> decorateWithExecutionInfoUpdater(
          DependencyInvocation<R2> invocationToDecorate) {
    return kryonCommand -> {
      if (kryonCommand instanceof DirectForwardSend directForwardSend) {
        @SuppressWarnings("unchecked")
        List<? extends RequestResponseFuture<? extends Request<Object>, Object>>
            executableRequests =
                (List<? extends RequestResponseFuture<? extends Request<Object>, Object>>)
                    directForwardSend.executableRequests();
        List<RequestResponseFuture<? extends Request<Object>, Object>> newReqRespFutures =
            new ArrayList<>(executableRequests.size());
        CompletableFuture[] newFutures = new CompletableFuture[executableRequests.size()];
        for (int i = 0; i < executableRequests.size(); i++) {
          RequestResponseFuture<? extends Request<Object>, Object> executableRequest =
              executableRequests.get(i);
          CompletableFuture<@Nullable Object> newFuture = new CompletableFuture<>();
          newReqRespFutures.add(
              new RequestResponseFuture<Request<Object>, Object>(
                  executableRequest.request(), newFuture));
          newFutures[i] = newFuture;
        }
        CompletableFuture.allOf(newFutures)
            .whenComplete(
                (unused, throwable) -> {
                  kryonExecutor.executionInfo().activeKryon(vajramID);
                  for (int i = 0; i < newFutures.length; i++) {
                    Futures.linkFutures(
                        newFutures[i],
                        executableRequests.get(i).response(),
                        kryonExecutor.commandQueue());
                  }
                });
        return invocationToDecorate.invokeDependency(
            (ClientSideCommand<R2>)
                new DirectForwardSend(
                    kryonCommand.vajramID(), newReqRespFutures, kryonCommand.dependentChain()));
      } else {
        return invocationToDecorate.invokeDependency(kryonCommand);
      }
    };
  }
}
