package com.flipkart.krystal.vajram.modulation;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.facets.InputValuesAdaptor;
import java.util.function.Function;

public interface InputsConverter<
        InputsNeedingModulation extends InputValuesAdaptor, CommonInputs extends InputValuesAdaptor>
    extends Function<Inputs, UnmodulatedInput<InputsNeedingModulation, CommonInputs>> {}
