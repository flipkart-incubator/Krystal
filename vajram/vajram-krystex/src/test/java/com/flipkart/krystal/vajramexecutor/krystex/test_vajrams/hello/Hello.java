package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloFacetUtil.HelloFacets;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

@VajramDef
public abstract class Hello extends ComputeVajram<String> {

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @Input String name;
  @Input Optional<String> greeting;

  @Output
  static String greet(HelloFacets facets) {
    CALL_COUNTER.increment();
    return "%s! %s".formatted(facets.greeting().orElse("Hello"), facets.name());
  }
}
