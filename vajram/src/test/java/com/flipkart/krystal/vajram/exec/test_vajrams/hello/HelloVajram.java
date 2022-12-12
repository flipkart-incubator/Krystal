package com.flipkart.krystal.vajram.exec.test_vajrams.hello;

import static com.flipkart.krystal.vajram.exec.test_vajrams.hello.HelloVajram.ID;
import static com.flipkart.krystal.vajram.inputs.Input.string;

import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.exec.test_vajrams.hello.HelloInputUtils.EnrichedRequest;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableList;

@VajramDef(ID)
public abstract class HelloVajram extends NonBlockingVajram<String> {

  public static final String ID = "flipkart.krystal.test_vajrams.HelloVajram";

  @Override
  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(string().name("name").mandatory().needsModulation().build());
  }

  @VajramLogic
  public String greet(EnrichedRequest inputs) {
    return "Hello! %s".formatted(inputs.name());
  }
}
