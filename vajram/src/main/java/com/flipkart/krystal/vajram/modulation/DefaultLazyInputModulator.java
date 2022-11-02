package com.flipkart.krystal.vajram.modulation;

import com.flipkart.krystal.vajram.inputs.InputModulator;
import com.flipkart.krystal.vajram.inputs.InputsConverter;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultLazyInputModulator<Request, InputsNeedingModulation, CommonInputs>
    implements InputModulator<Request, InputsNeedingModulation, CommonInputs> {

  private final Map<CommonInputs, List<InputsNeedingModulation>> unModulatedRequests =
      new HashMap<>();

  private final InputsConverter<Request, InputsNeedingModulation, CommonInputs> inputsConverter;

  public DefaultLazyInputModulator(
      InputsConverter<Request, InputsNeedingModulation, CommonInputs> inputsConverter) {
    this.inputsConverter = inputsConverter;
  }

  @Override
  public final ImmutableList<ModulatedInput<InputsNeedingModulation, CommonInputs>> add(
      Request request) {
    unModulatedRequests
        .computeIfAbsent(inputsConverter.extractCommonInputs(request), k -> new ArrayList<>())
        .add(inputsConverter.extractInputsNeedingModulation(request));
    return ImmutableList.of();
  }

  @Override
  public ImmutableList<ModulatedInput<InputsNeedingModulation, CommonInputs>> modulate() {
    ImmutableList<ModulatedInput<InputsNeedingModulation, CommonInputs>> result =
        unModulatedRequests.entrySet().stream()
            .map(e -> new ModulatedInput<>(e.getValue(), e.getKey()))
            .collect(ImmutableList.toImmutableList());
    unModulatedRequests.clear();
    return result;
  }
}
