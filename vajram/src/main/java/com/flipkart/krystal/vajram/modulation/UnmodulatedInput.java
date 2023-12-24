package com.flipkart.krystal.vajram.modulation;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.facets.InputValuesAdaptor;
import java.util.LinkedHashMap;
import java.util.Map;

public record UnmodulatedInput<
        InputsNeedingModulation extends InputValuesAdaptor,
        CommonInputs extends InputValuesAdaptor>(
    InputsNeedingModulation inputsNeedingModulation, CommonInputs commonInputs)
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
