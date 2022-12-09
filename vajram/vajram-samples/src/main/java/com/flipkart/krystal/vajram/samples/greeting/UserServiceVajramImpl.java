package com.flipkart.krystal.vajram.samples.greeting;

import com.flipkart.krystal.vajram.ModulatedExecutionContext;
import com.flipkart.krystal.vajram.modulation.InputModulator.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.flipkart.krystal.vajram.samples.greeting.UserServiceVajramInputUtils.CommonInputs;
import com.flipkart.krystal.vajram.samples.greeting.UserServiceVajramInputUtils.InputsNeedingModulation;
import com.flipkart.krystal.vajram.samples.greeting.UserServiceVajramInputUtils.ModulatedRequest;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;

public class UserServiceVajramImpl extends UserServiceVajram {

  @Override
  public ImmutableMap<?, CompletableFuture<UserInfo>> execute(
      ModulatedExecutionContext executionContext) {
    ModulatedInput<InputsNeedingModulation, CommonInputs> modulatedInput =
        executionContext.getModulatedInput();
    ModulatedRequest modulatedRequest =
        new ModulatedRequest(
            modulatedInput.inputsNeedingModulation(), modulatedInput.commonInputs());
    return ImmutableMap.copyOf(callUserService(modulatedRequest));
  }

  @Override
  public InputsConverter<?, ?, ?, ?> getInputsConvertor() {
    return UserServiceVajramInputUtils.CONVERTER;
  }
}
