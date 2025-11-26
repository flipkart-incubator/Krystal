package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.commands.KryonCommand;
import java.util.concurrent.CompletableFuture;

public interface Kryon<C extends KryonCommand<?>, R extends KryonCommandResponse> {

  CompletableFuture<R> executeCommand(C kryonCommand);

  VajramKryonDefinition getKryonDefinition();
}
