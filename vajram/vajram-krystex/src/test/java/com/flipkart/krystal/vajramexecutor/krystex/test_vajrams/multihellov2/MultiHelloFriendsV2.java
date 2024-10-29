package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2;

import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.dep;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.depInputFanout;
import static com.flipkart.krystal.vajram.facets.resolution.sdk.InputResolvers.resolve;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Facets.hellos_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Request.skip_s;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Request.userIds_s;

import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import com.google.common.collect.ImmutableCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@VajramDef
public abstract class MultiHelloFriendsV2 extends ComputeVajram<String> {
  static class _Facets {
    @Input Set<String> userIds;
    @Input Optional<Boolean> skip;

    @Dependency(onVajram = HelloFriendsV2.class, canFanout = true)
    String hellos;
  }

  @Override
  public ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return resolve(
        dep(
            hellos_s,
            depInputFanout(HelloFriendsV2Request.userId_s)
                .using(userIds_s, skip_s)
                .skipIf((userIds, skip) -> skip.valueOpt().orElse(false), "skip requested")
                .asResolver((userIds, skip) -> userIds.valueOpt().orElse(Set.of()))));
  }

  @Output
  static String sayHellos(MultiHelloFriendsV2Facets _allFacets) {
    if (_allFacets.skip().orElse(false)) {
      return "";
    }
    List<String> result = new ArrayList<>();
    for (var rr : _allFacets.hellos().requestResponses()) {
      rr.response().valueOpt().ifPresent(result::add);
    }
    return String.join(System.lineSeparator(), result);
  }
}
