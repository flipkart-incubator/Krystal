package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

@ExternallyInvocable
@Vajram
public abstract class Hello extends ComputeVajramDef<String> {
  static class _Inputs {
    @IfAbsent(FAIL)
    String name;

    String greeting;
  }

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @Output
  static String greet(Optional<String> greeting, String name) {
    CALL_COUNTER.increment();
    return "%s! %s".formatted(greeting.orElse("Hello"), name);
  }
}
