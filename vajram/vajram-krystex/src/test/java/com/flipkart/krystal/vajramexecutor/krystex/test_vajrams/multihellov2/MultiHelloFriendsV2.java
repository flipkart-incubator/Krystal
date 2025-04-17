package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2_Fac.hellos_n;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.except.SkippedExecutionException;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2_Req;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExternallyInvocable
@Vajram
public abstract class MultiHelloFriendsV2 extends ComputeVajramDef<String> {
  static class _Inputs {
    @IfNull(FAIL)
    Set<String> userIds;

    boolean skip;
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
      Optional<Boolean> skip, FanoutDepResponses<HelloFriendsV2_Req, String> hellos) {
    if (skip.orElse(false)) {
      return "";
    }
    List<String> result = new ArrayList<>();
    for (var rr : hellos.requestResponsePairs()) {
      rr.response().valueOpt().ifPresent(result::add);
    }
    return String.join(System.lineSeparator(), result);
  }
}
