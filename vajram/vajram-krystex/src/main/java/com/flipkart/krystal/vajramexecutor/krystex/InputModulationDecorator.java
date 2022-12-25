package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.vajramexecutor.krystex.Utils.toNodeInputs;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.krystex.MultiResultFuture;
import com.flipkart.krystal.krystex.node.NodeDecorator;
import com.flipkart.krystal.krystex.node.NodeLogic;
import com.flipkart.krystal.krystex.node.NodeLogicDefinition;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.modulation.InputModulator;
import com.flipkart.krystal.vajram.modulation.InputModulator.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputModulationDecorator<Request, InputsNeedingModulation, CommonInputs, T>
    implements NodeDecorator<T> {

  private final InputModulator<InputsNeedingModulation, CommonInputs> inputModulator;
  private final InputsConverter<Request, InputsNeedingModulation, CommonInputs> inputsConverter;
  private final Map<Request, MultiResultFuture<T>> futureCache = new HashMap<>();

  public InputModulationDecorator(
      InputModulator<InputsNeedingModulation, CommonInputs> inputModulator,
      InputsConverter<Request, InputsNeedingModulation, CommonInputs> inputsConverter) {
    this.inputModulator = inputModulator;
    this.inputsConverter = inputsConverter;
  }

  @Override
  public NodeLogic<T> decorateLogic(NodeLogicDefinition<T> nodeDef, NodeLogic<T> logicToDecorate) {
    inputModulator.onTermination(
        requests -> requests.forEach(request -> modulateInputsList(logicToDecorate, request)));
    return inputsList -> {
      List<Request> requests =
          inputsList.stream()
              .map(Utils::toInputValues)
              .map(inputsConverter::enrichedRequest)
              .toList();
      List<ModulatedInput<InputsNeedingModulation, CommonInputs>> modulatedInputs =
          requests.stream()
              .map(
                  request ->
                      inputModulator.add(
                          inputsConverter.inputsNeedingModulation(request),
                          inputsConverter.commonInputs(request)))
              .flatMap(Collection::stream)
              .toList();
      requests.forEach(
          request -> futureCache.computeIfAbsent(request, e -> new MultiResultFuture<>()));
      for (ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedInput : modulatedInputs) {
        modulateInputsList(logicToDecorate, modulatedInput);
      }
      return requests.stream()
          .collect(
              toImmutableMap(
                  request -> {
                    InputValues inputValues = inputsConverter.toMap(request);
                    return toNodeInputs(inputValues);
                  },
                  futureCache::get));
    };
  }

  private void modulateInputsList(
      NodeLogic<T> logicToDecorate,
      ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedInput) {
    ImmutableList<Request> requests =
        modulatedInput.inputsNeedingModulation().stream()
            .map(each -> inputsConverter.enrichedRequest(each, modulatedInput.commonInputs()))
            .collect(toImmutableList());
    logicToDecorate
        .apply(
            requests.stream()
                .map(enrichedRequest -> toNodeInputs(inputsConverter.toMap(enrichedRequest)))
                .collect(toImmutableList()))
        .forEach(
            (inputs, multiResult) -> {
              MultiResultFuture<T> cachedResult =
                  futureCache.computeIfAbsent(
                      inputsConverter.enrichedRequest(Utils.toInputValues(inputs)),
                      request -> new MultiResultFuture<>());
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
