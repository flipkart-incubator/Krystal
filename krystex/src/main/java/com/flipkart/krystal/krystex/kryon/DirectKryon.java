package com.flipkart.krystal.krystex.kryon;

import static com.flipkart.krystal.data.Errable.nil;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.failedFuture;

import com.flipkart.krystal.concurrent.Futures;
import com.flipkart.krystal.core.CommunicationFacade;
import com.flipkart.krystal.core.GraphExecutionData;
import com.flipkart.krystal.core.OutputLogicExecutionInput;
import com.flipkart.krystal.data.Errable;
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
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.request.InvocationId;
import com.flipkart.krystal.krystex.request.RequestIdGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DirectKryon
    extends AbstractKryon<MultiRequestDirectCommand<DirectResponse>, DirectResponse> {

  DirectKryon(
      VajramKryonDefinition definition,
      KryonExecutor kryonExecutor,
      Function<LogicExecutionContext, NavigableSet<OutputLogicDecorator>>
          sortedOutputLogicDecoratorsSupplier,
      Function<DependencyExecutionContext, ImmutableMap<String, DependencyDecorator>>
          depDecoratorSuppliers,
      DecorationOrdering decorationOrdering,
      RequestIdGenerator requestIdGenerator) {
    super(
        definition,
        kryonExecutor,
        sortedOutputLogicDecoratorsSupplier,
        depDecoratorSuppliers,
        decorationOrdering,
        requestIdGenerator);
  }

  @Override
  public CompletableFuture<DirectResponse> executeCommand(
      MultiRequestDirectCommand<DirectResponse> kryonCommand) {
    DependentChain dependentChain = kryonCommand.dependentChain();
    if (kryonCommand instanceof DirectForwardReceive directForwardReceive) {
      kryonDefinition.executeGraph(
          new GraphExecutionData(
              directForwardReceive.executableRequests(),
              new CommunicationFacade() {
                @Override
                public void triggerDependency(
                    Dependency dependency,
                    List<RequestResponseFuture<Request<@Nullable Object>, Object>>
                        requestResponseFutureList) {
                  kryonExecutor.executeCommand(
                      new DirectForwardSend(
                          dependency.onVajramID(),
                          requestResponseFutureList,
                          dependentChain.extend(kryonDefinition.vajramID(), dependency)));
                }

                @Override
                public void executeOutputLogic(ExecutionItem executionItem) {
                  executeDecoratedOutputLogic(
                      kryonDefinition.getOutputLogicDefinition(), executionItem, dependentChain);
                }
              },
              kryonExecutor.commandQueue()));
    }

    return CompletableFuture.completedFuture(DirectResponse.INSTANCE);
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
    CompletableFuture<@Nullable Object> result;
    try {
      result =
          finalLogic
              .execute(
                  new OutputLogicExecutionInput(
                      ImmutableList.of(executionItem.facetValues()), kryonExecutor.commandQueue()))
              .results()
              .values()
              .iterator()
              .next();
    } catch (Throwable e) {
      result = failedFuture(e);
    }
    Futures.linkFutures(result, executionItem.response());
  }
}
