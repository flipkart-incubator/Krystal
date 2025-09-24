package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import java.util.concurrent.CompletableFuture;

public interface Kryon<C extends KryonCommand, R extends KryonCommandResponse> {

  void flush(Flush flush);

  CompletableFuture<R> executeCommand(C kryonCommand);

  VajramKryonDefinition getKryonDefinition();
}
