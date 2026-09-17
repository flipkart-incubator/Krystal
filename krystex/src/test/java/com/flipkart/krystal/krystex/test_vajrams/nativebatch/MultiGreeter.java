package com.flipkart.krystal.krystex.test_vajrams.nativebatch;

import static com.flipkart.krystal.krystex.test_vajrams.nativebatch.MultiGreeter_Fac.greetings_n;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;

/**
 * Fans out to multiple concurrent invocations of {@link Greeter}, so that {@code Greeter} is
 * invoked as a {@code ForwardReceiveBatch}, letting tests verify that {@code
 * NativeInjectingDecoratedKryon} merges injected facets into every invocation in the batch.
 */
@InvocableOutsideGraph
@Vajram
public abstract class MultiGreeter extends ComputeVajramDef<String> {
  interface _Inputs {
    @IfAbsent(FAIL)
    List<String> names();
  }

  interface _InternalFacets {
    @Dependency(onVajram = Greeter.class, canFanout = true)
    String greetings();
  }

  @Resolve(dep = greetings_n, depInputs = Greeter_Req.name_n)
  static FanoutCommand<String> namesForGreetings(List<String> names) {
    return executeFanoutWith(ImmutableSet.copyOf(names));
  }

  @Output
  static String allGreetings(FanoutDepResponses<Greeter_Req, String> greetings) {
    return greetings.requestResponsePairs().stream()
        .map(rr -> rr.response().valueOpt())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(joining(", "));
  }
}
