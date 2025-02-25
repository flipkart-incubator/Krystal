package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

@ExternalInvocation(allow = true)
@Vajram
public abstract class Hello extends ComputeVajramDef<String> {
  static class _Facets {
    @Mandatory @Input String name;
    @Input String greeting;
  }

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @Output
  static String greet(Optional<String> greeting, String name) {
    CALL_COUNTER.increment();
    return "%s! %s".formatted(greeting.orElse("Hello"), name);
  }
}
