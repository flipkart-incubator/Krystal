package com.flipkart.krystal.vajram.samples.greeting;

public final class UserServiceVajramImpl123 {
//  @Override
//  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
//    return ImmutableList.of(
//        Input.builder()
//            // Local name for this input
//            .name("user_id")
//            // Data type - used for code generation
//            .type(string())
//            // If this input is not provided by the client, throw a build time error.
//            .isMandatory()
//            .needsModulation()
//            .build());
//  }
//
//  @Override
//  public InputsConverter<UserServiceInputsNeedingModulation, UserServiceCommonInputs> getInputsConvertor() {
//    return CONVERTER;
//  }
//
//  @Override
//  public ImmutableMap<Inputs, CompletableFuture<UserInfo>> execute(
//      ImmutableList<Inputs> inputsList) {
//    Map<UserServiceInputsNeedingModulation, Inputs> mapping = new HashMap<>();
//    List<UserServiceInputsNeedingModulation> ims = new ArrayList<>();
//      UserServiceCommonInputs commonInputs = null;
//    for (Inputs inputs : inputsList) {
//      UnmodulatedInput<UserServiceInputsNeedingModulation, UserServiceCommonInputs> allInputs =
//          getInputsConvertor().apply(inputs);
//      commonInputs = allInputs.commonInputs();
//        UserServiceInputsNeedingModulation im = allInputs.inputsNeedingModulation();
//      mapping.put(im, inputs);
//      ims.add(im);
//    }
//    Map<Inputs, CompletableFuture<UserInfo>> returnValue = new LinkedHashMap<>();
//
//    if (commonInputs != null) {
//      var results = callUserService(new ModulatedInput<>(ImmutableList.copyOf(ims), commonInputs));
//      results.forEach((im, future) -> returnValue.put(mapping.get(im), future));
//    }
//    return ImmutableMap.copyOf(returnValue);
//  }
}
