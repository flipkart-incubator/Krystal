package com.flipkart.krystal.vajram.modulation;

import com.google.common.collect.ImmutableList;

public record ModulatedInput<InputsNeedingModulation, CommonInputs>(
    ImmutableList<InputsNeedingModulation> modInputs, CommonInputs commonInputs) {}
