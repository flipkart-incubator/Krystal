package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2_Fac.hellos_n;
import static java.util.Objects.requireNonNullElse;

import com.flipkart.krystal.annos.InvocableOutsideGraph;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.except.KrystalCompletionException;
import com.flipkart.krystal.except.SkippedExecutionException;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2_Req;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

@InvocableOutsideGraph
@Vajram
public abstract class MultiHelloFriendsV2 extends ComputeVajramDef<String> {
  static class _Inputs {
    @IfAbsent(FAIL)
    Set<String> userIds;

    boolean skip;

    @IfAbsent(ASSUME_DEFAULT_VALUE)
    boolean fail;
  }

  static class _InternalFacets {
    @Dependency(onVajram = HelloFriendsV2.class, canFanout = true)
    String hellos;
  }

  @Resolve(dep = hellos_n, depInputs = HelloFriendsV2_Req.userId_n)
  static FanoutCommand<String> userIdsForHellos(Set<String> userIds, Optional<Boolean> skip) {
    if (skip.orElse(false)) {
      throw new SkippedExecutionException("skip requested");
    }
    return executeFanoutWith(userIds);
  }

  @Output
  static String sayHellos(
      @Nullable Boolean skip, FanoutDepResponses<HelloFriendsV2_Req, String> hellos, boolean fail) {
    if (fail) {
      throw new KrystalCompletionException("Fail requested");
    }
    if (requireNonNullElse(skip, false)) {
      return "";
    }
    List<String> result = new ArrayList<>();
    hellos.forEach((request, response) -> response.valueOpt().ifPresent(result::add));
    return String.join(System.lineSeparator(), result);
  }
}
