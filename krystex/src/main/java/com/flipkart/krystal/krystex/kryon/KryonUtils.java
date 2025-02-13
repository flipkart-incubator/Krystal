package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import java.util.function.Supplier;

public final class KryonUtils {

  @SuppressWarnings("FutureReturnValueIgnored")
  static void enqueueOrExecuteCommand(
      Supplier<KryonCommand> commandGenerator,
      KryonId depKryonId,
      KryonDefinition kryonDefinition,
      KryonExecutor kryonExecutor) {
    OutputLogicDefinition<Object> depOutputLogic =
        kryonDefinition.kryonDefinitionRegistry().get(depKryonId).getOutputLogicDefinition();
    if (depOutputLogic instanceof IOLogicDefinition<Object>) {
      kryonExecutor.enqueueKryonCommand(commandGenerator);
    } else if (depOutputLogic instanceof ComputeLogicDefinition<Object>) {
      kryonExecutor.executeCommand(commandGenerator.get());
    } else {
      throw new UnsupportedOperationException(
          "Unknown logicDefinition type %s".formatted(depOutputLogic.getClass()));
    }
  }

  private KryonUtils() {}
}
