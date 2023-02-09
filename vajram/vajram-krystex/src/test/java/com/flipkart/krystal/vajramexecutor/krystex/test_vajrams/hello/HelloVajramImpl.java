package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import static com.flipkart.krystal.datatypes.StringType.string;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloInputUtil.AllInputs;
import com.google.common.collect.ImmutableList;

// Auto generated and managed by Krystal
public final class HelloVajramImpl extends HelloVajram {

  @Override
  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("name").type(string()).isMandatory().build(),
        Input.builder().name("greeting").type(string()).build());
  }

  @Override
  public String executeCompute(Inputs i) {
    return greet(
        new AllInputs(i.getInputValueOrThrow("name"), i.getInputValueOrDefault("greeting", null)));
  }
}
