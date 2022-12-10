package com.flipkart.krystal.vajram.exec;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.krystex.IoNodeAdaptor;
import com.flipkart.krystal.krystex.NodeInputs;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.modulation.InputModulator;
import com.flipkart.krystal.vajram.modulation.InputModulator.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class InputModulationAdaptor<
        Request,
        InputsNeedingModulation,
        CommonInputs,
        Modulated extends ModulatedInput<InputsNeedingModulation, CommonInputs>,
        T>
    implements IoNodeAdaptor<T> {

  private final InputModulator<Request, InputsNeedingModulation, CommonInputs> inputModulator;
  private final InputsConverter<Request, InputsNeedingModulation, CommonInputs, Modulated>
      inputsConverter;
  private final Map<Request, CompletableFuture<ImmutableList<T>>> futureCache = new HashMap<>();

  public InputModulationAdaptor(
      InputModulator<Request, InputsNeedingModulation, CommonInputs> inputModulator,
      InputsConverter<Request, InputsNeedingModulation, CommonInputs, Modulated> inputsConverter) {
    this.inputModulator = inputModulator;
    this.inputsConverter = inputsConverter;
  }

  @Override
  public Function<NodeInputs, CompletableFuture<ImmutableList<T>>> adaptLogic(
      Function<ImmutableList<NodeInputs>, ImmutableMap<NodeInputs, CompletableFuture<T>>>
          logicToDecorate) {

    inputModulator.onTermination(requests -> modulateInputsList(logicToDecorate, requests));

    return inputs -> {
      Request request = inputsConverter.enrichedRequest(new InputValues(inputs.values()));
      ImmutableList<ModulatedInput<InputsNeedingModulation, CommonInputs>> modulatedInputs =
          inputModulator.add(request);
      futureCache.computeIfAbsent(request, e -> new CompletableFuture<>());
      for (ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedInput : modulatedInputs) {
        modulateInputsList(logicToDecorate, modulatedInput);
      }
      return futureCache.get(request);
    };
  }

  private void modulateInputsList(
      Function<ImmutableList<NodeInputs>, ImmutableMap<NodeInputs, CompletableFuture<T>>>
          logicToDecorate,
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
            (inputs, future) -> {
              CompletableFuture<ImmutableList<T>> cachedFuture =
                  futureCache.computeIfAbsent(
                      inputsConverter.enrichedRequest(new InputValues(inputs.values())),
                      request -> new CompletableFuture<>());
              future.whenComplete(
                  (t, throwable) -> {
                    if (t != null) {
                      cachedFuture.complete(ImmutableList.of(t));
                    } else {
                      cachedFuture.completeExceptionally(throwable);
                    }
                  });
            });
  }
}
