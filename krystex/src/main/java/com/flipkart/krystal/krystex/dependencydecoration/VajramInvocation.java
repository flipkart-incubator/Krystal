package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.krystex.commands.ClientSideCommand;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface VajramInvocation<R extends KryonCommandResponse> {
  CompletableFuture<R> invokeDependency(ClientSideCommand<R> kryonCommand);
}
