package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import java.util.concurrent.CompletableFuture;

sealed interface Kryon<C extends KryonCommand, R extends KryonResponse> permits AbstractKryon {

  void executeCommand(Flush flushCommand);

  CompletableFuture<R> executeCommand(C kryonCommand);
}
