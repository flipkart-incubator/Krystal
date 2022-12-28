package com.flipkart.krystal.vajram.codegen.samples.hello;

import static com.flipkart.krystal.datatypes.StringType.string;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableList;

@VajramDef(HelloCodeGenVajram.ID)
public class HelloCodeGenVajram extends ComputeVajram<String> {

  public static final String ID = "flipkart.krystal.vajram.codegen.samples.HelloCodeGenVajram";

  @Override
  public ImmutableList<VajramInputDefinition> getInputDefinitions() {
    return ImmutableList.of(
        Input.builder().name("name").type(string()).isMandatory().build(),
        Input.builder().name("greeting").type(string()).build());
  }
}
