package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloInputUtil.HelloAllInputs;
import java.util.concurrent.atomic.LongAdder;

@VajramDef(HelloVajram.ID)
public abstract class HelloVajram extends ComputeVajram<String> {

  public static final String ID = "flipkart.krystal.test_vajrams.HelloVajram";

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @VajramLogic
  public String greet(HelloAllInputs inputs) {
    CALL_COUNTER.increment();
    return "%s! %s".formatted(inputs.greeting().orElse("Hello"), inputs.name());
  }
}
