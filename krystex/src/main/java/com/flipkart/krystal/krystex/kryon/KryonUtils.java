package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import java.util.Optional;
import java.util.function.Supplier;

public final class KryonUtils {

  @SuppressWarnings("FutureReturnValueIgnored")
  static void enqueueOrExecuteCommand(
      Supplier<KryonCommand> commandGenerator,
      VajramID depVajramID,
      KryonDefinition kryonDefinition,
      KryonExecutor kryonExecutor) {
    Optional<KryonDefinition> kryonDefinitionOpt =
        kryonDefinition.kryonDefinitionRegistry().tryGet(depVajramID);
    if (kryonDefinitionOpt.isEmpty()) {
      kryonExecutor.enqueueKryonCommand(commandGenerator);
    } else {
      OutputLogicDefinition<Object> depOutputLogic =
          kryonDefinitionOpt.get().getOutputLogicDefinition();
      if (depOutputLogic instanceof IOLogicDefinition<Object>) {
        kryonExecutor.enqueueKryonCommand(commandGenerator);
      } else if (depOutputLogic instanceof ComputeLogicDefinition<Object>) {
        kryonExecutor.executeCommand(commandGenerator.get());
      } else {
        throw new UnsupportedOperationException(
            "Unknown logicDefinition type %s".formatted(depOutputLogic.getClass()));
      }
    }
  }

  private KryonUtils() {}
}
