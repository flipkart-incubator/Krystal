package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.flipkart.krystal.utils.ImmutableMapView;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class KryonUtils {

  static ImmutableMapView<Optional<String>, List<ResolverDefinition>>
      createResolverDefinitionsByInputs(ImmutableList<ResolverDefinition> resolverDefinitions) {
    Map<Optional<String>, List<ResolverDefinition>> resolverDefinitionsByInput =
        new LinkedHashMap<>();
    resolverDefinitions.forEach(
        resolverDefinition -> {
          if (!resolverDefinition.boundFrom().isEmpty()) {
            resolverDefinition
                .boundFrom()
                .forEach(
                    input ->
                        resolverDefinitionsByInput
                            .computeIfAbsent(Optional.of(input), s -> new ArrayList<>())
                            .add(resolverDefinition));
          } else {
            resolverDefinitionsByInput
                .computeIfAbsent(Optional.empty(), s -> new ArrayList<>())
                .add(resolverDefinition);
          }
        });
    return ImmutableMapView.viewOf(resolverDefinitionsByInput);
  }

  static void enqueueOrExecuteCommand(
      Supplier<KryonCommand> commandGenerator,
      KryonId depKryonId,
      KryonDefinition kryonDefinition,
      KryonExecutor kryonExecutor) {
    MainLogicDefinition<Object> depMainLogic =
        kryonDefinition.kryonDefinitionRegistry().get(depKryonId).getMainLogicDefinition();
    if (depMainLogic instanceof IOLogicDefinition<Object>) {
      kryonExecutor.enqueueKryonCommand(commandGenerator);
    } else if (depMainLogic instanceof ComputeLogicDefinition<Object>) {
      kryonExecutor.executeCommand(commandGenerator.get());
    } else {
      throw new UnsupportedOperationException(
          "Unknown logicDefinition type %s".formatted(depMainLogic.getClass()));
    }
  }

  private KryonUtils() {}
}
