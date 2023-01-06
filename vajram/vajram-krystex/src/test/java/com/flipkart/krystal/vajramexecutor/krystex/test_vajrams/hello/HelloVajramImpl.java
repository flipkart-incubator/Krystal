package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import static com.flipkart.krystal.datatypes.StringType.string;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloInputUtil.AllInputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

// Auto generated and managed by Krystal
public class HelloVajramImpl extends HelloVajram {

  @Override
  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("name").type(string()).isMandatory().build(),
        Input.builder().name("greeting").type(string()).build());
  }

  @Override
  public ImmutableMap<InputValues, String> executeCompute(ImmutableList<InputValues> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                i -> i,
                i -> greet(new AllInputs(i.getOrThrow("name"), i.getOrDefault("greeting", null)))));
  }
}
