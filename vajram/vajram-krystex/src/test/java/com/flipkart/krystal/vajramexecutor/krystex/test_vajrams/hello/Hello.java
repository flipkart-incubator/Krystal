package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloInputUtil.HelloInputs;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

@VajramDef
public abstract class Hello extends ComputeVajram<String> {

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @Input String name;
  @Input Optional<String> greeting;

  @VajramLogic
  static String greet(HelloInputs inputs) {
    CALL_COUNTER.increment();
    return "%s! %s".formatted(inputs.greeting().orElse("Hello"), inputs.name());
  }
}
