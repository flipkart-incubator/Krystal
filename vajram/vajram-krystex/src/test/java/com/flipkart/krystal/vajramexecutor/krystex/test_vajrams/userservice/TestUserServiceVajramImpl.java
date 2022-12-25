package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice;

import com.flipkart.krystal.vajram.ModulatedExecutionContext;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;

public class TestUserServiceVajramImpl extends TestUserServiceVajram {

  @Override
  public ImmutableMap<?, CompletableFuture<TestUserInfo>> execute(
      ModulatedExecutionContext executionContext) {
    return ImmutableMap.copyOf(callUserService(executionContext.getModulatedInput()));
  }

  @Override
  public InputsConverter<?, ?, ?> getInputsConvertor() {
    return TestUserServiceInputUtils.CONVERTER;
  }
}
