package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.except.KrystalException.wrapAsCompletionException;

import com.flipkart.krystal.core.CommunicationFacade;
import com.flipkart.krystal.core.GraphExecutionData;
import com.flipkart.krystal.core.OutputLogicExecutionInput;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.DirectForwardReceive;
import com.flipkart.krystal.krystex.commands.DirectForwardSend;
import com.flipkart.krystal.krystex.commands.MultiRequestDirectCommand;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyExecutionContext;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyInvocation;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class DirectKryon extends AbstractKryon<MultiRequestDirectCommand, DirectResponse> {

  DirectKryon(
      VajramKryonDefinition definition,
      KryonExecutor kryonExecutor,
      Function<LogicExecutionContext, NavigableSet<OutputLogicDecorator>>
          sortedOutputLogicDecoratorsSupplier,
      Function<DependencyExecutionContext, ImmutableMap<String, DependencyDecorator>>
          depDecoratorSuppliers,
      DecorationOrdering decorationOrdering) {
    super(
        definition,
        kryonExecutor,
        sortedOutputLogicDecoratorsSupplier,
        depDecoratorSuppliers,
        decorationOrdering);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  @Override
  public CompletableFuture<DirectResponse> executeCommand(MultiRequestDirectCommand kryonCommand) {
    DependentChain dependentChain = kryonCommand.dependentChain();
    VajramID vajramID = kryonDefinition.vajramID();

    if (kryonCommand instanceof DirectForwardReceive directForwardReceive) {
      List<ExecutionItem> executionItems = directForwardReceive.executionItems();

      try {
        CommunicationFacade communicationFacade =
            new CommunicationFacade() {
              @Override
              public void triggerDependency(
                  Dependency dependency,
                  List<? extends RequestResponseFuture<? extends Request<?>, ?>>
                      requestResponseFutureList) {
                DependentChain extendedDependentChain = dependentChain.extend(vajramID, dependency);
                DependencyInvocation<DirectResponse> kryonResponseDependencyInvocation =
                    decorateVajramInvocation(
                        extendedDependentChain,
                        dependency.onVajramID(),
                        kryonExecutor::executeCommand);
                kryonResponseDependencyInvocation.invokeDependency(
                    new DirectForwardSend(
                        dependency.onVajramID(),
                        requestResponseFutureList,
                        extendedDependentChain));
              }

              @Override
              public void executeOutputLogic() {
                for (ExecutionItem executionItem : executionItems) {
                  executeDecoratedOutputLogic(
                      kryonDefinition.getOutputLogicDefinition(), executionItem, dependentChain);
                }
                flushDecorators(dependentChain);
              }
            };
        if (executionItems.isEmpty()) {
          // This means this vajram was skipped.
          // Propagate this information to all dependencies by calling them with no requests
          // So that this dependent chain are flushed all the way
          kryonDefinition
              .dependencies()
              .forEach(
                  dependency ->
                      kryonExecutor.executeCommand(
                          new DirectForwardSend(
                              dependency.onVajramID(),
                              ImmutableList.of(),
                              dependentChain.extend(vajramID, dependency))));
          flushDecorators(dependentChain);
        }
        kryonDefinition.executeGraph(
            new GraphExecutionData(
                executionItems, communicationFacade, kryonExecutor.commandQueue()));
      } catch (Throwable e) {
        for (ExecutionItem executionItem : executionItems) {
          if (!executionItem.response().isDone()) {
            executionItem.response().completeExceptionally(wrapAsCompletionException(e));
          }
        }
      }
    }

    return CompletableFuture.completedFuture(DirectResponse.instance());
  }

  private void executeDecoratedOutputLogic(
      OutputLogicDefinition<Object> outputLogicDefinition,
      ExecutionItem executionItem,
      DependentChain dependentChain) {
    OutputLogic<Object> logic = outputLogicDefinition.logic();

    for (OutputLogicDecorator outputLogicDecorator :
        getSortedOutputLogicDecorators(dependentChain)) {
      logic = outputLogicDecorator.decorateLogic(logic, outputLogicDefinition);
    }
    OutputLogic<Object> finalLogic = logic;
    try {
      finalLogic.execute(
          new OutputLogicExecutionInput(
              ImmutableList.of(executionItem), kryonExecutor.commandQueue()));
    } catch (Throwable e) {
      executionItem.response().completeExceptionally(wrapAsCompletionException(e));
    }
  }
}
