package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import static com.flipkart.krystal.data.ValueOrError.withValue;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderInputUtil.CONVERTER;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.datatypes.IntegerType;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.modulation.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.UnmodulatedInput;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderInputUtil.AdderCommonInputs;
import com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.AdderInputUtil.AdderInputsNeedingModulation;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AdderImpl extends Adder {

  @Override
  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("number_one").type(IntegerType.integer()).isMandatory().build(),
        Input.builder().name("number_two").type(IntegerType.integer()).build());
  }

  @Override
  public InputsConverter<AdderInputUtil.AdderInputsNeedingModulation, AdderCommonInputs>
      getInputsConvertor() {
    return CONVERTER;
  }

  @Override
  public ImmutableMap<Inputs, ValueOrError<Integer>> executeCompute(
      ImmutableList<Inputs> inputsList) {
    Map<AdderInputUtil.AdderInputsNeedingModulation, Inputs> mapping = new HashMap<>();
    List<AdderInputsNeedingModulation> ims = new ArrayList<>();
      AdderCommonInputs commonInputs = null;
    for (Inputs inputs : inputsList) {
      UnmodulatedInput<AdderInputsNeedingModulation, AdderCommonInputs> allInputs =
          getInputsConvertor().apply(inputs);
      commonInputs = allInputs.commonInputs();
        AdderInputsNeedingModulation im = allInputs.inputsNeedingModulation();
      mapping.put(im, inputs);
      ims.add(im);
    }
    Map<Inputs, ValueOrError<Integer>> returnValue = new LinkedHashMap<>();

    if (commonInputs != null) {
      var results = add(new ModulatedInput<>(ImmutableList.copyOf(ims), commonInputs));
      results.forEach((im, future) -> returnValue.put(mapping.get(im), withValue(future)));
    }
    return ImmutableMap.copyOf(returnValue);
  }
}
