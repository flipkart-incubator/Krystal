package com.flipkart.krystal.lattice.graphql.rest.dispatch;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.core.VajramInvocation;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.krystex.commands.ClientSideCommand;
import com.flipkart.krystal.krystex.commands.DirectForwardSend;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecoratorConfig;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyInvocation;
import com.flipkart.krystal.krystex.kryon.DirectResponse;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationError;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate_Req;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import graphql.ExecutionInput;
import graphql.GraphQL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GraphQlOperationDelegator implements DependencyDecorator, KryonExecutorConfigurator {

  public static final String DECORATOR_TYPE = GraphQlOperationDelegator.class.getName();
  private final GraphQL graphQL;
  private final VajramKryonGraph graph;

  public GraphQlOperationDelegator(GraphQL graphQL, VajramKryonGraph graph) {
    this.graphQL = graphQL;
    this.graph = graph;
  }

  @Override
  public <R extends KryonCommandResponse> DependencyInvocation<R> decorateDependency(
      DependencyInvocation<R> invocationToDecorate) {
    return kryonCommand -> {
      if (!(kryonCommand instanceof DirectForwardSend directForwardSend)) {
        log.error(
            "Only DirectForwardSend supported by {}. Forwarding the command as is",
            decoratorType());
        return invocationToDecorate.invokeDependency(kryonCommand);
      }
      List<? extends RequestResponseFuture<? extends Request<?>, ?>> requestResponseFutures =
          directForwardSend.executableRequests();
      if (requestResponseFutures.size() > 1) {
        String msg = "As per GraphQl spec, only one operation is allowed to execute at a time";
        log.error(msg);
        return CompletableFuture.failedFuture(new IllegalArgumentException(msg));
      } else if (requestResponseFutures.isEmpty()) {
        log.error("No requests found. Forwarding message as is");
        return invocationToDecorate.invokeDependency(kryonCommand);
      }

      @SuppressWarnings("unchecked")
      RequestResponseFuture<? extends Request<?>, GraphQlOperationObject> requestResponseFuture =
          (RequestResponseFuture<? extends Request<?>, GraphQlOperationObject>)
              requestResponseFutures.get(0);
      Request<?> request = requestResponseFuture.request();
      if (!(request instanceof GraphQlOperationAggregate_Req<?> graphQlOperationAggregateReq)) {
        log.error(
            """
                {} only supports requests of type {}. \
                This decorator should only we used on dependencies on vajram. {} \
                This seems to be a configuration error. Forwarding command as-is.""",
            decoratorType(),
            GraphQlOperationAggregate_Req.class,
            GraphQlOperationAggregate.class);
        return invocationToDecorate.invokeDependency(kryonCommand);
      }
      ExecutionInput executionInput = graphQlOperationAggregateReq.executionInput();
      if (executionInput == null) {
        log.error(
            "Could not delegate GraphQlOperationAggregate_Req because executionInput was null. Forwarding command as is");
        return invocationToDecorate.invokeDependency(kryonCommand);
      }

      graphQL
          .executeAsync(
              executionInput.transform(
                  execInputBuilder ->
                      execInputBuilder.graphQLContext(
                          Map.of(
                              VajramExecutionStrategy.VAJRAM_INVOCATION_CTX_KEY,
                              (VajramInvocation<GraphQlOperationObject>)
                                  computedRequestResponseFuture -> {
                                    Request<GraphQlOperationObject> computedRequest =
                                        computedRequestResponseFuture.request();
                                    @SuppressWarnings("unchecked")
                                    ClientSideCommand<R> newCommand =
                                        (ClientSideCommand<R>)
                                            new DirectForwardSend(
                                                computedRequest._vajramID(),
                                                List.of(
                                                    new RequestResponseFuture<>(
                                                        computedRequest,
                                                        requestResponseFuture.response())),
                                                kryonCommand.dependentChain());

                                    invocationToDecorate.invokeDependency(newCommand);
                                  }))))
          .whenComplete(
              (executionResult, throwable) -> {
                if (throwable != null) {
                  // This means that there was some error because of which the Aggregation vajram
                  // was never invoked. Propagate the error to the response
                  requestResponseFuture.response().completeExceptionally(throwable);
                } else {
                  if (!requestResponseFuture.response().isDone()) {
                    // This means the execution was aborted before the Aggregation vajram was
                    // invoked. Propagate the state to the response.
                    requestResponseFuture
                        .response()
                        .complete(GraphQlOperationError.from(executionResult));
                  }
                }
              });

      return completedFuture(DirectResponse.instance());
    };
  }

  @Override
  public void addToConfig(KryonExecutorConfigBuilder configBuilder) {
    configBuilder.dependencyDecoratorConfig(
        DECORATOR_TYPE,
        new DependencyDecoratorConfig(
            DECORATOR_TYPE,
            dependencyExecutionContext -> {
              Optional<Class<? extends Request<?>>> depVajramReq =
                  graph.getVajramReqByVajramId(dependencyExecutionContext.depVajramId());
              return depVajramReq.isPresent()
                  && GraphQlOperationAggregate_Req.class.isAssignableFrom(depVajramReq.get());
            },
            _c -> DECORATOR_TYPE,
            _c -> this));
  }
}
