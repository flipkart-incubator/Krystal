package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.utils.Futures.propagateCancellation;
import static com.flipkart.krystal.vajramexecutor.krystex.Utils.toNodeInputs;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.krystex.node.MainLogic;
import com.flipkart.krystal.krystex.node.MainLogicDecorator;
import com.flipkart.krystal.krystex.node.MainLogicDefinition;
import com.flipkart.krystal.krystex.node.NodeInputs;
import com.flipkart.krystal.vajram.inputs.InputValues;
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

public class InputModulationDecorator<
        InputsNeedingModulation extends InputValuesAdaptor,
        CommonInputs extends InputValuesAdaptor,
        T>
    implements MainLogicDecorator<T> {

  private final InputModulator<InputsNeedingModulation, CommonInputs> inputModulator;
  private final InputsConverter<InputsNeedingModulation, CommonInputs> inputsConverter;
  private final Map<InputValues, CompletableFuture<T>> futureCache = new HashMap<>();

  public InputModulationDecorator(
      InputModulator<InputsNeedingModulation, CommonInputs> inputModulator,
      InputsConverter<InputsNeedingModulation, CommonInputs> inputsConverter) {
    this.inputModulator = inputModulator;
    this.inputsConverter = inputsConverter;
  }

  @Override
  public MainLogic<T> decorateLogic(MainLogicDefinition<T> nodeDef, MainLogic<T> logicToDecorate) {
    inputModulator.onTermination(
        requests -> requests.forEach(request -> modulateInputsList(logicToDecorate, request)));
    return inputsList -> {
      List<UnmodulatedInput<InputsNeedingModulation, CommonInputs>> requests =
          inputsList.stream().map(Utils::toInputValues).map(inputsConverter).toList();
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
          .collect(toImmutableMap(Utils::toNodeInputs, futureCache::get));
    };
  }

  private void modulateInputsList(
      MainLogic<T> logicToDecorate,
      ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedInput) {
    ImmutableList<UnmodulatedInput<InputsNeedingModulation, CommonInputs>> requests =
        modulatedInput.inputsNeedingModulation().stream()
            .map(each -> new UnmodulatedInput<>(each, modulatedInput.commonInputs()))
            .collect(toImmutableList());
    ImmutableMap<NodeInputs, CompletableFuture<T>> originalFutures =
        logicToDecorate.execute(
            requests.stream()
                .map(enrichedRequest -> toNodeInputs(enrichedRequest.toInputValues()))
                .collect(toImmutableList()));
    originalFutures.forEach(
        (inputs, resultFuture) -> {
          CompletableFuture<T> cachedResult =
              futureCache.computeIfAbsent(
                  Utils.toInputValues(inputs), request -> new CompletableFuture<>());
          resultFuture.whenComplete(
              (values, throwable) -> {
                if (values != null) {
                  cachedResult.complete(values);
                } else {
                  cachedResult.completeExceptionally(throwable);
                }
              });
          propagateCancellation(cachedResult, resultFuture);
        });
  }
}
