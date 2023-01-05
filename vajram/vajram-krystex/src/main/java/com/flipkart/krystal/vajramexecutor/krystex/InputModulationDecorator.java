package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.vajramexecutor.krystex.Utils.toNodeInputs;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.krystex.MultiResultFuture;
import com.flipkart.krystal.krystex.node.NodeDecorator;
import com.flipkart.krystal.krystex.node.NodeLogic;
import com.flipkart.krystal.krystex.node.NodeLogicDefinition;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import com.flipkart.krystal.vajram.modulation.InputModulator;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.UnmodulatedInput;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputModulationDecorator<
        InputsNeedingModulation extends InputValuesAdaptor,
        CommonInputs extends InputValuesAdaptor,
        T>
    implements NodeDecorator<T> {

  private final InputModulator<InputsNeedingModulation, CommonInputs> inputModulator;
  private final InputsConverter<InputsNeedingModulation, CommonInputs> inputsConverter;
  private final Map<InputValues, MultiResultFuture<T>> futureCache = new HashMap<>();

  public InputModulationDecorator(
      InputModulator<InputsNeedingModulation, CommonInputs> inputModulator,
      InputsConverter<InputsNeedingModulation, CommonInputs> inputsConverter) {
    this.inputModulator = inputModulator;
    this.inputsConverter = inputsConverter;
  }

  @Override
  public NodeLogic<T> decorateLogic(NodeLogicDefinition<T> nodeDef, NodeLogic<T> logicToDecorate) {
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
              futureCache.computeIfAbsent(request.toInputValues(), e -> new MultiResultFuture<>()));
      for (ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedInput : modulatedInputs) {
        modulateInputsList(logicToDecorate, modulatedInput);
      }
      return requests.stream()
          .map(UnmodulatedInput::toInputValues)
          .collect(toImmutableMap(Utils::toNodeInputs, futureCache::get));
    };
  }

  private void modulateInputsList(
      NodeLogic<T> logicToDecorate,
      ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedInput) {
    ImmutableList<UnmodulatedInput<InputsNeedingModulation, CommonInputs>> requests =
        modulatedInput.inputsNeedingModulation().stream()
            .map(each -> new UnmodulatedInput<>(each, modulatedInput.commonInputs()))
            .collect(toImmutableList());
    logicToDecorate
        .apply(
            requests.stream()
                .map(enrichedRequest -> toNodeInputs(enrichedRequest.toInputValues()))
                .collect(toImmutableList()))
        .forEach(
            (inputs, multiResult) -> {
              MultiResultFuture<T> cachedResult =
                  futureCache.computeIfAbsent(
                      Utils.toInputValues(inputs), request -> new MultiResultFuture<>());
              multiResult
                  .future()
                  .whenComplete(
                      (values, throwable) -> {
                        if (values != null) {
                          cachedResult.future().complete(values);
                        } else {
                          cachedResult.future().completeExceptionally(throwable);
                        }
                      });
            });
  }
}
