package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.commands.KryonCommand;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractKryonDecorator implements KryonDecorator {

  protected final CompletableFuture<KryonResponse> executeCommand(
      KryonCommand kryonCommand, KryonExecutor kryonExecutor) {
    return kryonExecutor.executeCommand(kryonCommand);
  }
}
