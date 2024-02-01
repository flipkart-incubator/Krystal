package com.flipkart.krystal.vajram.modulation;

import com.google.common.collect.ImmutableList;

public record ModulatedFacets<InputsNeedingModulation, CommonFacets>(
    ImmutableList<InputsNeedingModulation> modInputs, CommonFacets commonFacets) {}
