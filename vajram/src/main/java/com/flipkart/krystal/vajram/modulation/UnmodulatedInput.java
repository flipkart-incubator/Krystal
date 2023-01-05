package com.flipkart.krystal.vajram.modulation;

import com.flipkart.krystal.vajram.ValueOrError;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;

public record UnmodulatedInput<
        InputsNeedingModulation extends InputValuesAdaptor,
        CommonInputs extends InputValuesAdaptor>(
    @NonNull InputsNeedingModulation inputsNeedingModulation, @NonNull CommonInputs commonInputs)
    implements InputValuesAdaptor {

  @Override
  public InputValues toInputValues() {
    ImmutableMap<String, ValueOrError<?>> imValues =
        inputsNeedingModulation.toInputValues().values();
    ImmutableMap<String, ValueOrError<?>> ciValues = commonInputs.toInputValues().values();
    Map<String, ValueOrError<?>> merged = new LinkedHashMap<>(imValues);
    merged.putAll(ciValues);
    return new InputValues(ImmutableMap.copyOf(merged));
  }
}
