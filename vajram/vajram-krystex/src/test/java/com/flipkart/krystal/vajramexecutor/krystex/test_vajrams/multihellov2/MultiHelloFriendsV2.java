package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2;

import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInputFanout;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Facets.hellos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Request.skip_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Request.userIds_s;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.except.SkippedExecutionException;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import com.google.common.collect.ImmutableCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExternalInvocation(allow = true)
@VajramDef
public abstract class MultiHelloFriendsV2 extends ComputeVajram<String> {
  static class _Facets {
    @Input Set<String> userIds;
    @Input Optional<Boolean> skip;

    @Dependency(onVajram = HelloFriendsV2.class, canFanout = true)
    String hellos;
  }

  @Override
  public ImmutableCollection<SimpleInputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            hellos_s,
            depInputFanout(HelloFriendsV2Request.userId_s)
                .using(userIds_s, skip_s)
                .asResolver(
                    (userIds, skip) -> {
                      if (skip.valueOpt().orElse(false)) {
                        throw new SkippedExecutionException("skip requested");
                      }
                      return userIds.valueOpt().orElse(Set.of());
                    })));
  }

  @Output
  static String sayHellos(
      Optional<Boolean> skip, FanoutDepResponses hellos) {
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
