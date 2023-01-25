package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.utils.Futures.linkEndStates;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.config.NestedConfig;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.decoration.LogicDecoratorCommand;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.decoration.TerminateDecoration;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import com.flipkart.krystal.vajram.modulation.InputModulator;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.UnmodulatedInput;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class InputModulationDecorator<
        InputsNeedingModulation extends InputValuesAdaptor, CommonInputs extends InputValuesAdaptor>
    implements MainLogicDecorator {

  public static final String DECORATOR_TYPE = InputModulationDecorator.class.getName();

  private final String instanceId;
  private final InputModulator<InputsNeedingModulation, CommonInputs> inputModulator;
  private final InputsConverter<InputsNeedingModulation, CommonInputs> inputsConverter;
  private final Map<Inputs, CompletableFuture<Object>> futureCache = new HashMap<>();
  private boolean terminated;

  public InputModulationDecorator(
      String instanceId,
      InputModulator<InputsNeedingModulation, CommonInputs> inputModulator,
      InputsConverter<InputsNeedingModulation, CommonInputs> inputsConverter) {
    this.instanceId = instanceId;
    this.inputModulator = inputModulator;
    this.inputsConverter = inputsConverter;
  }

  @Override
  public MainLogic<Object> decorateLogic(MainLogic<Object> logicToDecorate) {
    inputModulator.onTermination(
        requests -> {
          if (!terminated) {
            requests.forEach(request -> modulateInputsList(logicToDecorate, request));
          }
          terminated = true;
        });
    return inputsList -> {
      List<UnmodulatedInput<InputsNeedingModulation, CommonInputs>> requests =
          inputsList.stream().map(inputsConverter).toList();
      List<ModulatedInput<InputsNeedingModulation, CommonInputs>> modulatedInputs =
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
              futureCache.computeIfAbsent(request.toInputValues(), e -> new CompletableFuture<>()));
      for (ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedInput : modulatedInputs) {
        modulateInputsList(logicToDecorate, modulatedInput);
      }
      return requests.stream()
          .map(UnmodulatedInput::toInputValues)
          .collect(toImmutableMap(Function.identity(), futureCache::get));
    };
  }

  @Override
  public void executeCommand(Inputs inputs, LogicDecoratorCommand logicDecoratorCommand) {
    if (logicDecoratorCommand instanceof TerminateDecoration) {
      var unmodulatedInput = inputsConverter.apply(inputs);
      inputModulator.terminate(
          unmodulatedInput.inputsNeedingModulation(), unmodulatedInput.commonInputs());
    }
  }

  private void modulateInputsList(
      MainLogic<Object> logicToDecorate,
      ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedInput) {
    ImmutableList<UnmodulatedInput<InputsNeedingModulation, CommonInputs>> requests =
        modulatedInput.inputsNeedingModulation().stream()
            .map(each -> new UnmodulatedInput<>(each, modulatedInput.commonInputs()))
            .collect(toImmutableList());
    ImmutableMap<Inputs, CompletableFuture<Object>> originalFutures =
        logicToDecorate.execute(
            requests.stream().map(UnmodulatedInput::toInputValues).collect(toImmutableList()));
    originalFutures.forEach(
        (inputs, resultFuture) -> {
          CompletableFuture<Object> cachedResult =
              futureCache.computeIfAbsent(inputs, request -> new CompletableFuture<>());
          linkEndStates(resultFuture, cachedResult);
        });
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    inputModulator.onConfigUpdate(new NestedConfig("input_modulation.", configProvider));
  }

  @Override
  public String getId() {
    return instanceId;
  }
}
