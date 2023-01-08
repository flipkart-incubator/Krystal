package com.flipkart.krystal.vajram.modulation;

import com.flipkart.krystal.data.InputValues;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import java.util.function.Function;

public interface InputsConverter<
        InputsNeedingModulation extends InputValuesAdaptor, CommonInputs extends InputValuesAdaptor>
    extends Function<InputValues, UnmodulatedInput<InputsNeedingModulation, CommonInputs>> {}
