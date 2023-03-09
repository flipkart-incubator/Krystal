package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

public final class AdderImpl123 {

  //  @Override
  //  public ImmutableCollection<VajramInputDefinition> getInputDefinitions() {
  //    return ImmutableList.of(
  //        Input.builder().name("number_one").type(IntegerType.integer()).isMandatory().build(),
  //        Input.builder().name("number_two").type(IntegerType.integer()).build());
  //  }
  //
  //  @Override
  //  public InputsConverter<AdderInputUtil.AdderInputsNeedingModulation, AdderCommonInputs>
  //      getInputsConvertor() {
  //    return CONVERTER;
  //  }
  //
  //  @Override
  //  public ImmutableMap<Inputs, ValueOrError<Integer>> executeCompute(
  //      ImmutableList<Inputs> inputsList) {
  //    Map<AdderInputUtil.AdderInputsNeedingModulation, Inputs> mapping = new HashMap<>();
  //    List<AdderInputsNeedingModulation> ims = new ArrayList<>();
  //      AdderCommonInputs commonInputs = null;
  //    for (Inputs inputs : inputsList) {
  //      UnmodulatedInput<AdderInputsNeedingModulation, AdderCommonInputs> allInputs =
  //          getInputsConvertor().apply(inputs);
  //      commonInputs = allInputs.commonInputs();
  //        AdderInputsNeedingModulation im = allInputs.inputsNeedingModulation();
  //      mapping.put(im, inputs);
  //      ims.add(im);
  //    }
  //    Map<Inputs, ValueOrError<Integer>> returnValue = new LinkedHashMap<>();
  //
  //    if (commonInputs != null) {
  //      var results = add(new ModulatedInput<>(ImmutableList.copyOf(ims), commonInputs));
  //      results.forEach((im, future) -> returnValue.put(mapping.get(im), withValue(future)));
  //    }
  //    return ImmutableMap.copyOf(returnValue);
  //  }
}
