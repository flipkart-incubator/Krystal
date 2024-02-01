package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2;

import static com.flipkart.krystal.vajram.facets.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Request.hellos_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Request.skip_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Request.userIds_n;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.Dependency;
import com.flipkart.krystal.vajram.Input;
import com.flipkart.krystal.vajram.Output;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2FacetUtil.MultiHelloFriendsV2Facets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@VajramDef
public abstract class MultiHelloFriendsV2 extends ComputeVajram<String> {

  @Input Set<String> userIds;
  @Input Optional<Boolean> skip;

  @Dependency(onVajram = HelloFriendsV2.class, canFanout = true)
  String hellos;

  @Resolve(depName = hellos_n, depInputs = HelloFriendsV2Request.userId_n)
  public static MultiExecute<String> userIdsForHellos(
      @Using(userIds_n) Set<String> userIds, @Using(skip_n) Optional<Boolean> shouldSkip) {
    if (shouldSkip.orElse(false)) {
      return skipFanout("skip requested");
    }
    return executeFanoutWith(userIds);
  }

  @Output
  static String sayHellos(MultiHelloFriendsV2Facets facets) {
    if (facets.skip().orElse(false)) {
      return "";
    }
    Set<String> userIds = facets.userIds();
    List<String> result = new ArrayList<>();
    for (String userId : userIds) {
      facets
          .hellos()
          .get(HelloFriendsV2Request.builder().userId(userId).build())
          .value()
          .ifPresent(result::add);
    }
    return String.join(System.lineSeparator(), result);
  }
}
