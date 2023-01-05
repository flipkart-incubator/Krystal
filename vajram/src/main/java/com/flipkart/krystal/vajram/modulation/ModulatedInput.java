package com.flipkart.krystal.vajram.modulation;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.NonNull;

public record ModulatedInput<InputsNeedingModulation, CommonInputs>(
    @NonNull ImmutableList<InputsNeedingModulation> inputsNeedingModulation,
    @NonNull CommonInputs commonInputs) {

}
