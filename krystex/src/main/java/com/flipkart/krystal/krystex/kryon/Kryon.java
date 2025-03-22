package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import java.util.concurrent.CompletableFuture;

public interface Kryon<C extends KryonCommand, R extends KryonCommandResponse> {

  void executeCommand(Flush flushCommand);

  CompletableFuture<R> executeCommand(C kryonCommand);

  VajramKryonDefinition getKryonDefinition();
}
