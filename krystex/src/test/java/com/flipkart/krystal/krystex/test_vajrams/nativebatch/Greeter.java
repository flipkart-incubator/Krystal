package com.flipkart.krystal.krystex.test_vajrams.nativebatch;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A leaf vajram with a native-injectable facet, used only to exercise {@code
 * NativeInjectingDecoratedKryon}'s batch (fanout) code path via {@link MultiGreeter}.
 */
@InvocableOutsideGraph
@Vajram
public abstract class Greeter extends ComputeVajramDef<String> {
  interface _Inputs {
    @IfAbsent(FAIL)
    String name();
  }

  interface _InternalFacets {
    @Inject
    String greetingPrefix();
  }

  @Output
  static String greet(String name, @Nullable String greetingPrefix) {
    return (greetingPrefix != null ? greetingPrefix : "Hello") + " " + name;
  }
}
