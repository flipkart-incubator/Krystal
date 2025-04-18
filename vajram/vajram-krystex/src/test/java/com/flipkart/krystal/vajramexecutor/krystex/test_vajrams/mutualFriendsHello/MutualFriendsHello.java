package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.skipFanout;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHello_Fac.friendIds_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHello_Fac.hellos_n;
import static java.lang.System.lineSeparator;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsService_Req;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2_Req;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExternallyInvocable
@Vajram
public abstract class MutualFriendsHello extends ComputeVajramDef<String> {
  static class _Inputs {
    @IfNull(FAIL)
    String userId;

    boolean skip;
  }

  static class _InternalFacets {
    @IfNull(FAIL)
    @Dependency(onVajram = FriendsService.class)
    Set<String> friendIds;

    @Dependency(onVajram = HelloFriendsV2.class, canFanout = true)
    String hellos;
  }

  @Resolve(dep = friendIds_n, depInputs = FriendsService_Req.userId_n)
  public static String userIdForFriendService(String userId) {
    return userId;
  }

  @Resolve(dep = hellos_n, depInputs = HelloFriendsV2_Req.userId_n)
  public static FanoutCommand<String> userIDForHelloService(
      Set<String> friendIds, Optional<Boolean> skip) {
    if (skip.orElse(false)) {
      return skipFanout("skip requested");
    }
    return executeFanoutWith(friendIds);
  }

  @Output
  static String sayHelloToMutualFriends(FanoutDepResponses<String, HelloFriendsV2_Req> hellos) {
    List<String> result = new ArrayList<>();
    for (var response : hellos.requestResponsePairs()) {
      response.response().valueOpt().ifPresent(result::add);
    }
    return String.join(lineSeparator(), result);
  }
}
