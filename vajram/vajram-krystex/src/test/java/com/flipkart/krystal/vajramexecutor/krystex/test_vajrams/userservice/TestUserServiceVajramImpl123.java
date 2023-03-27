package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice;

public final class TestUserServiceVajramImpl123 {
  //  @Override
  //  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
  //    return ImmutableList.of(
  //        Input.builder()
  //            // Local name for this input
  //            .name(USER_ID)
  //            // Data type - used for code generation
  //            .type(string())
  //            // If this input is not provided by the client, throw a build time error.
  //            .isMandatory()
  //            .needsModulation()
  //            .build());
  //  }
  //
  //  @Override
  //  public ImmutableMap<Inputs, CompletableFuture<TestUserInfo>> execute(
  //      ImmutableList<Inputs> inputsList) {
  //    Map<TestUserServiceInputsNeedingModulation, Inputs> mapping = new HashMap<>();
  //    List<TestUserServiceInputsNeedingModulation> ims = new ArrayList<>();
  //    TestUserServiceCommonInputs commonInputs = null;
  //    for (Inputs inputs : inputsList) {
  //      UnmodulatedInput<TestUserServiceInputsNeedingModulation, TestUserServiceCommonInputs>
  //          allInputs = getInputsConvertor().apply(inputs);
  //      commonInputs = allInputs.commonInputs();
  //      TestUserServiceInputsNeedingModulation im = allInputs.inputsNeedingModulation();
  //      mapping.put(im, inputs);
  //      ims.add(im);
  //    }
  //    Map<Inputs, CompletableFuture<TestUserInfo>> returnValue = new LinkedHashMap<>();
  //
  //    if (commonInputs != null) {
  //      ImmutableMap<TestUserServiceInputsNeedingModulation, CompletableFuture<TestUserInfo>>
  //          results = callUserService(new ModulatedInput<>(ImmutableList.copyOf(ims),
  // commonInputs));
  //      results.forEach((im, future) -> returnValue.put(mapping.get(im), future));
  //    }
  //    return ImmutableMap.copyOf(returnValue);
  //  }
  //
  //  @Override
  //  public InputsConverter<TestUserServiceInputsNeedingModulation, TestUserServiceCommonInputs>
  //      getInputsConvertor() {
  //    return CONVERTER;
  //  }
}
