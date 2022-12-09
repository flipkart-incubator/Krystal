package com.flipkart.krystal.vajram.exec.test_vajrams.userservice;

import com.flipkart.krystal.vajram.ModulatedExecutionContext;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserServiceVajramInputUtils.CommonInputs;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserServiceVajramInputUtils.InputsNeedingModulation;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserServiceVajramInputUtils.ModulatedRequest;
import com.flipkart.krystal.vajram.modulation.InputModulator.ModulatedInput;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;

public class TestUserServiceVajramImpl extends TestUserServiceVajram {

  @Override
  public ImmutableMap<?, CompletableFuture<TestUserInfo>> execute(
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
    return TestUserServiceVajramInputUtils.CONVERTER;
  }
}
