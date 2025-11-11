package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.commands.KryonCommand;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class KryonUtils {

  static CompletableFuture<?> enqueueOrExecuteCommand(
      Supplier<KryonCommand<?>> commandGenerator, KryonExecutor kryonExecutor) {
    if (kryonExecutor.singleThreadExecutor().isCurrentThreadTheSingleThread()) {
      return kryonExecutor.executeCommand(commandGenerator.get());
    } else {
      return kryonExecutor.enqueueKryonCommand(commandGenerator);
    }
  }

  static VajramKryonDefinition validateAsVajram(KryonDefinition kryonDefinition) {
    if (!(kryonDefinition instanceof VajramKryonDefinition vajramKryonDefinition)) {
      throw new IllegalStateException(
          "Kryon command execution is supported only for vajrams. Found: "
              + kryonDefinition.getClass()
              + " VajramId: "
              + kryonDefinition.vajramID());
    }
    return vajramKryonDefinition;
  }

  private KryonUtils() {}
}
