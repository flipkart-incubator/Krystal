package com.flipkart.krystal.krystex.kryon;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.groupingBy;

import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.KryonDefinition.KryonDefinitionView;
import com.flipkart.krystal.krystex.resolution.ResolverDefinition;
import com.flipkart.krystal.model.KryonId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;

public final class KryonUtils {

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

  static KryonDefinitionView toView(
      ImmutableList<ResolverDefinition> resolverDefinitions,
      ImmutableMap<String, KryonId> dependencyKryons) {
    ImmutableMap<String, ImmutableSet<ResolverDefinition>> resolverDefinitionsByDependencies =
        ImmutableMap.copyOf(
            resolverDefinitions.stream()
                .collect(groupingBy(ResolverDefinition::dependencyName, toImmutableSet())));
    ImmutableSet<String> dependenciesWithNoResolvers =
        dependencyKryons.keySet().stream()
            .filter(
                depName ->
                    resolverDefinitionsByDependencies
                        .getOrDefault(depName, ImmutableSet.of())
                        .isEmpty())
            .collect(toImmutableSet());
    return new KryonDefinitionView(
        createResolverDefinitionsByInputs(resolverDefinitions),
        resolverDefinitionsByDependencies,
        dependenciesWithNoResolvers);
  }

  private static ImmutableMap<Optional<String>, ImmutableSet<ResolverDefinition>>
      createResolverDefinitionsByInputs(ImmutableList<ResolverDefinition> resolverDefinitions) {
    Map<Optional<String>, ImmutableSet.Builder<ResolverDefinition>> resolverDefinitionsByInput =
        new LinkedHashMap<>();
    resolverDefinitions.forEach(
        resolverDefinition -> {
          if (!resolverDefinition.boundFrom().isEmpty()) {
            resolverDefinition
                .boundFrom()
                .forEach(
                    input ->
                        resolverDefinitionsByInput
                            .computeIfAbsent(Optional.of(input), s -> ImmutableSet.builder())
                            .add(resolverDefinition));
          } else {
            resolverDefinitionsByInput
                .computeIfAbsent(Optional.empty(), s -> ImmutableSet.builder())
                .add(resolverDefinition);
          }
        });
    return resolverDefinitionsByInput.entrySet().stream()
        .collect(toImmutableMap(Entry::getKey, e -> e.getValue().build()));
  }

  private KryonUtils() {}
}
