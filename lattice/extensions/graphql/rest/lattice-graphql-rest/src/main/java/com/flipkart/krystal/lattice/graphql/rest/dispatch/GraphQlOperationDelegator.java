package com.flipkart.krystal.lattice.graphql.rest.dispatch;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.core.VajramInvocation;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.krystex.commands.ClientSideCommand;
import com.flipkart.krystal.krystex.commands.DirectForwardSend;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyInvocation;
import com.flipkart.krystal.krystex.kryon.DirectResponse;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate_Req;
import graphql.ExecutionInput;
import graphql.GraphQL;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GraphQlOperationDelegator implements DependencyDecorator {

  private final GraphQL graphQL;

  @Inject
  public GraphQlOperationDelegator(GraphQL graphQL) {
    this.graphQL = graphQL;
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
        return CompletableFuture.failedFuture(new IllegalArgumentException("msg"));
      } else if (requestResponseFutures.isEmpty()) {
        log.error("No requests found. Forwarding message as is");
        invocationToDecorate.invokeDependency(kryonCommand);
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

      graphQL.executeAsync(
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
                              }))));
      return completedFuture(DirectResponse.instance());
    };
  }
}
