package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.krystex.Decorator;
import com.flipkart.krystal.krystex.commands.ClientSideCommand;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public non-sealed interface DependencyDecorator extends Decorator {

  <R extends KryonResponse> CompletableFuture<R> invokeDependency(
      ClientSideCommand<R> kryonCommand, VajramInvocation<R> invocationToDecorate);

  public static final DependencyDecorator NO_OP =
      new DependencyDecorator() {
        @Override
        public <R extends KryonResponse> CompletableFuture<R> invokeDependency(
            ClientSideCommand<R> kryonCommand, VajramInvocation<R> invocationToDecorate) {
          return invocationToDecorate.invokeDependency(kryonCommand);
        }
      };
}
