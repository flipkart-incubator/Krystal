package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello;

import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.skipFanout;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHello_Fac.friendIds_i;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHello_Fac.hellos_i;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHello_Fac.skip_i;
import static java.lang.System.lineSeparator;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsService_Req;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2_Req;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExternalInvocation(allow = true)
@VajramDef
public abstract class MutualFriendsHello extends ComputeVajram<String> {
  static class _Facets {
    @Mandatory @Input String userId;
    @Input boolean skip;

    @Mandatory
    @Dependency(onVajram = FriendsService.class)
    Set<String> friendIds;

    @Dependency(onVajram = HelloFriendsV2.class, canFanout = true)
    String hellos;
  }

  @Resolve(dep = friendIds_i, depInputs = FriendsService_Req.userId_i)
  public static String userIdForFriendService(String userId) {
    return userId;
  }

  @Resolve(dep = hellos_i, depInputs = HelloFriendsV2_Req.userId_i)
  public static FanoutCommand<String> userIDForHelloService(
      @Using(friendIds_i) Set<String> friendIds, @Using(skip_i) Optional<Boolean> skip) {
    if (skip.orElse(false)) {
      return skipFanout("skip requested");
    }
    return executeFanoutWith(friendIds);
  }

  @Output
  static String sayHelloToMutualFriends(FanoutDepResponses<HelloFriendsV2_Req, String> hellos) {
    List<String> result = new ArrayList<>();
    for (var response : hellos.requestResponsePairs()) {
      response.response().valueOpt().ifPresent(result::add);
    }
    return String.join(lineSeparator(), result);
  }
}
