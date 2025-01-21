package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.commands.ServerSideCommand;
import java.util.concurrent.CompletableFuture;

public interface Kryon<C extends KryonCommand, R extends KryonResponse> {

  void executeCommand(Flush flushCommand);

  CompletableFuture<R> executeCommand(C kryonCommand);

  KryonDefinition getKryonDefinition();
}
