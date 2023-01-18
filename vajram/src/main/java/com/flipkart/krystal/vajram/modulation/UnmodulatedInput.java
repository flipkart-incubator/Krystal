package com.flipkart.krystal.vajram.modulation;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;

public record UnmodulatedInput<
        InputsNeedingModulation extends InputValuesAdaptor,
        CommonInputs extends InputValuesAdaptor>(
    @NonNull InputsNeedingModulation inputsNeedingModulation, @NonNull CommonInputs commonInputs)
    implements InputValuesAdaptor {

  @Override
  public Inputs toInputValues() {
    Map<String, InputValue<Object>> imValues = inputsNeedingModulation.toInputValues().values();
    Map<String, InputValue<Object>> ciValues = commonInputs.toInputValues().values();
    LinkedHashMap<String, InputValue<Object>> merged = new LinkedHashMap<>(imValues);
    merged.putAll(ciValues);
    return new Inputs(merged);
  }
}
