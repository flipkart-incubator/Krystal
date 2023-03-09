package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import static com.flipkart.krystal.data.ValueOrError.valueOrError;
import static com.flipkart.krystal.datatypes.StringType.string;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloInputUtil.HelloAllInputs;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

// Auto generated and managed by Krystal
public final class HelloVajramImpl extends HelloVajram {

  @Override
  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("name").type(string()).isMandatory().build(),
        Input.builder().name("greeting").type(string()).build());
  }

  @Override
  public ImmutableMap<Inputs, ValueOrError<String>> executeCompute(
      ImmutableList<Inputs> inputsList) {
    return inputsList.stream()
        .collect(
            toImmutableMap(
                i -> i,
                i ->
                    valueOrError(
                        () ->
                            greet(
                                new HelloAllInputs(
                                    i.getInputValueOrThrow("name"),
                                    i.getInputValueOrDefault("greeting", null))))));
  }
}
