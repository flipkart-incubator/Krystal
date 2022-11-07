package com.flipkart.krystal.vajram.exec.test_vajrams.hello;

import static com.flipkart.krystal.vajram.exec.test_vajrams.hello.HelloVajram.ID;
import static com.flipkart.krystal.vajram.inputs.Input.string;

import com.flipkart.krystal.vajram.NonBlockingVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.exec.test_vajrams.hello.InputUtils.AllInputs;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import java.util.List;

@VajramDef(ID)
public abstract class HelloVajram extends NonBlockingVajram<String> {

  public static final String ID = "flipkart.krystal.test_vajrams.HelloVajram";

  @Override
  public List<VajramInputDefinition> getInputDefinitions() {
    return List.of(string().name("name").mandatory().needsModulation().build());
  }

  @VajramLogic
  public String greet(AllInputs inputs) {
    return "Hello! %s".formatted(inputs.name());
  }
}
