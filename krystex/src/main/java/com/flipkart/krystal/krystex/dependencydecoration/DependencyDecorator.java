package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.krystex.Decorator;
import com.flipkart.krystal.krystex.commands.ClientSideCommand;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import java.util.concurrent.CompletableFuture;

public non-sealed interface DependencyDecorator extends Decorator {

  <R extends KryonResponse> CompletableFuture<R> invokeDependency(
      ClientSideCommand<R> kryonCommand, VajramInvocation<R> invocationToDecorate);
}
