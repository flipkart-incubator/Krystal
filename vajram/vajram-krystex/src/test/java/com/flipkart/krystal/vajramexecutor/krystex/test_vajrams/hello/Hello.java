package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

@ExternalInvocation(allow = true)
@VajramDef
public abstract class Hello extends ComputeVajram<String> {
  static class _Facets {
    @Input String name;
    @Input Optional<String> greeting;
  }

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @Output
  static String greet(Optional<String> greeting, String name) {
    CALL_COUNTER.increment();
    return "%s! %s".formatted(greeting.orElse("Hello"), name);
  }
}
