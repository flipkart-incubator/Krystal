package com.flipkart.krystal.vajram.exec.test_vajrams.friendsservice;

import static com.flipkart.krystal.vajram.exec.test_vajrams.friendsservice.FriendsServiceInputUtils.CONVERTER;

import com.flipkart.krystal.vajram.ModulatedExecutionContext;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FriendsServiceVajramImpl extends FriendsServiceVajram {

  @Override
  public ImmutableMap<?, CompletableFuture<Set<String>>> execute(
      ModulatedExecutionContext executionContext) {
    return ImmutableMap.copyOf(call(executionContext.getModulatedInput()));
  }

  @Override
  public InputsConverter<?, ?, ?> getInputsConvertor() {
    return CONVERTER;
  }
}
