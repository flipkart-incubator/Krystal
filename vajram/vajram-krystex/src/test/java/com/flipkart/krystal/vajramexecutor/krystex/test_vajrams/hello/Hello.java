package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.VajramDef;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;

@VajramDef
public abstract class Hello extends ComputeVajram<String> {
  static class _Facets {
    @Input String name;
    @Input Optional<String> greeting;
  }

  public static final LongAdder CALL_COUNTER = new LongAdder();

  @Output
  static String greet(HelloFacets _allFacets) {
    CALL_COUNTER.increment();
    return "%s! %s".formatted(_allFacets.greeting().orElse("Hello"), _allFacets.name());
  }
}
