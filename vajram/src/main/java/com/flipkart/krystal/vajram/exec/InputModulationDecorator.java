package com.flipkart.krystal.vajram.exec;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.krystex.MultiResult;
import com.flipkart.krystal.krystex.NodeDecorator;
import com.flipkart.krystal.krystex.NodeDefinition;
import com.flipkart.krystal.krystex.NodeInputs;
import com.flipkart.krystal.krystex.NodeLogic;
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
  private final Map<Request, MultiResult<T>> futureCache = new HashMap<>();

  public InputModulationDecorator(
      InputModulator<InputsNeedingModulation, CommonInputs> inputModulator,
      InputsConverter<Request, InputsNeedingModulation, CommonInputs> inputsConverter) {
    this.inputModulator = inputModulator;
    this.inputsConverter = inputsConverter;
  }

  @Override
  public NodeLogic<T> decorateLogic(NodeDefinition<T> nodeDef, NodeLogic<T> logicToDecorate) {
    inputModulator.onInternalTermination(requests -> modulateInputsList(logicToDecorate, requests));
    return inputsList -> {
      List<Request> requests =
          inputsList.stream()
              .map(nodeInputs -> new InputValues(nodeInputs.values()))
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
      requests.forEach(request -> futureCache.computeIfAbsent(request, e -> new MultiResult<>()));
      for (ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedInput : modulatedInputs) {
        modulateInputsList(logicToDecorate, modulatedInput);
      }
      return requests.stream()
          .collect(
              toImmutableMap(
                  request -> new NodeInputs(inputsConverter.toMap(request).values()),
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
                .map(
                    enrichedRequest ->
                        new NodeInputs(inputsConverter.toMap(enrichedRequest).values()))
                .collect(toImmutableList()))
        .forEach(
            (inputs, multiResult) -> {
              MultiResult<T> cachedResult =
                  futureCache.computeIfAbsent(
                      inputsConverter.enrichedRequest(new InputValues(inputs.values())),
                      request -> new MultiResult<>());
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
