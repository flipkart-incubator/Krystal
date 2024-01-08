package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.utils.Futures.linkFutures;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.config.NestedConfig;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.decoration.InitiateActiveDepChains;
import com.flipkart.krystal.krystex.decoration.LogicDecoratorCommand;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.vajram.facets.InputValuesAdaptor;
import com.flipkart.krystal.vajram.modulation.InputModulator;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.UnmodulatedInput;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputModulationDecorator<
        I /*InputsNeedingModulation*/ extends InputValuesAdaptor,
        C /*CommonInputs*/ extends InputValuesAdaptor>
    implements MainLogicDecorator {

  public static final String DECORATOR_TYPE = InputModulationDecorator.class.getName();
  private final String instanceId;
  private final InputModulator<I, C> inputModulator;
  private final InputsConverter<I, C> inputsConverter;
  private final Predicate<DependantChain> isApplicableToDependantChain;
  private final Map<Inputs, CompletableFuture<@Nullable Object>> futureCache = new HashMap<>();
  private ImmutableSet<DependantChain> activeDependantChains = ImmutableSet.of();
  private final Set<DependantChain> flushedDependantChains = new LinkedHashSet<>();

  public InputModulationDecorator(
      String instanceId,
      InputModulator<I, C> inputModulator,
      InputsConverter<I, C> inputsConverter,
      Predicate<DependantChain> isApplicableToDependantChain) {
    this.instanceId = instanceId;
    this.inputModulator = inputModulator;
    this.inputsConverter = inputsConverter;
    this.isApplicableToDependantChain = isApplicableToDependantChain;
  }

  @Override
  public MainLogic<Object> decorateLogic(
      MainLogic<Object> logicToDecorate, MainLogicDefinition<Object> originalLogicDefinition) {
    inputModulator.onModulation(
        requests -> requests.forEach(request -> modulateInputsList(logicToDecorate, request)));
    return inputsList -> {
      List<UnmodulatedInput<I, C>> requests = inputsList.stream().map(inputsConverter).toList();
      List<ModulatedInput<I, C>> modulatedInputs =
          requests.stream()
              .map(
                  unmodulatedInput ->
                      inputModulator.add(
                          unmodulatedInput.inputsNeedingModulation(),
                          unmodulatedInput.commonInputs()))
              .flatMap(Collection::stream)
              .toList();
      requests.forEach(
          request ->
              futureCache.computeIfAbsent(
                  request.toInputValues(), e -> new CompletableFuture<@Nullable Object>()));
      for (ModulatedInput<I, C> modulatedInput : modulatedInputs) {
        modulateInputsList(logicToDecorate, modulatedInput);
      }
      return requests.stream()
          .map(UnmodulatedInput::toInputValues)
          .collect(
              ImmutableMap.<Inputs, Inputs, CompletableFuture<@Nullable Object>>toImmutableMap(
                  Function.identity(),
                  key ->
                      Optional.ofNullable(futureCache.get(key))
                          .orElseThrow(
                              () ->
                                  new AssertionError(
                                      "Future cache has been primed with values. This should never happen"))));
    };
  }

  @Override
  public void executeCommand(LogicDecoratorCommand logicDecoratorCommand) {
    if (logicDecoratorCommand instanceof InitiateActiveDepChains initiateActiveDepChains) {
      LinkedHashSet<DependantChain> allActiveDepChains =
          new LinkedHashSet<>(initiateActiveDepChains.dependantsChains());
      // Retain only the ones which are applicable for this input modulation decorator
      allActiveDepChains.removeIf(isApplicableToDependantChain.negate());
      this.activeDependantChains = ImmutableSet.copyOf(allActiveDepChains);
    } else if (logicDecoratorCommand instanceof FlushCommand flushCommand) {
      flushedDependantChains.add(flushCommand.dependantsChain());
      if (flushedDependantChains.containsAll(activeDependantChains)) {
        inputModulator.modulate();
        flushedDependantChains.clear();
      }
    }
  }

  private void modulateInputsList(
      MainLogic<Object> logicToDecorate, ModulatedInput<I, C> modulatedInput) {
    ImmutableList<UnmodulatedInput<I, C>> requests =
        modulatedInput.modInputs().stream()
            .map(each -> new UnmodulatedInput<>(each, modulatedInput.commonInputs()))
            .collect(toImmutableList());
    logicToDecorate
        .execute(requests.stream().map(UnmodulatedInput::toInputValues).collect(toImmutableList()))
        .forEach(
            (inputs, resultFuture) -> {
              //noinspection RedundantTypeArguments: To Handle nullChecker errors
              linkFutures(
                  resultFuture,
                  futureCache.<CompletableFuture<@Nullable Object>>computeIfAbsent(
                      inputs, request -> new CompletableFuture<@Nullable Object>()));
            });
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    inputModulator.onConfigUpdate(
        new NestedConfig(String.format("input_modulation.%s.", instanceId), configProvider));
  }

  @Override
  public String getId() {
    return instanceId;
  }
}
