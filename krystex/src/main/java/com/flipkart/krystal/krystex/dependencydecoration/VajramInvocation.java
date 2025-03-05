package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.krystex.commands.ClientSideCommand;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface VajramInvocation<R extends KryonResponse> {
  CompletableFuture<R> invokeDependency(ClientSideCommand<R> kryonCommand);
}
