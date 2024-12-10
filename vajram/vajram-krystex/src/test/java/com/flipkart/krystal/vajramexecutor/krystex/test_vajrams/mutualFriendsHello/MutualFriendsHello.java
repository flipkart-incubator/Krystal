package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello;

import static com.flipkart.krystal.vajram.facets.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHelloFacets.friendIds_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHelloFacets.hellos_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHelloRequest.skip_n;
import static java.lang.System.lineSeparator;

import com.flipkart.krystal.annos.ExternalInvocation;
import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.Using;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ExternalInvocation(allow = true)
@VajramDef
public abstract class MutualFriendsHello extends ComputeVajram<String> {
  static class _Facets {
    @Input String userId;

    @Input Optional<Boolean> skip;

    @Dependency(onVajram = FriendsService.class)
    Set<String> friendIds;

    @Dependency(onVajram = HelloFriendsV2.class, canFanout = true)
    String hellos;
  }

  @Resolve(depName = friendIds_n, depInputs = FriendsServiceRequest.userId_n)
  public static String userIdForFriendService(String userId) {
    return userId;
  }

  @Resolve(depName = hellos_n, depInputs = HelloFriendsV2Request.userId_n)
  public static MultiExecute<String> userIDForHelloService(
      @Using(friendIds_n) Set<String> friendIds, @Using(skip_n) Optional<Boolean> skip) {
    if (skip.orElse(false)) {
      return skipFanout("skip requested");
    }
    return executeFanoutWith(friendIds);
  }

  @Output
  static String sayHelloToMutualFriends(MutualFriendsHelloFacets _allFacets) {
    List<String> result = new ArrayList<>();
    for (var response : _allFacets.hellos().requestResponses()) {
      response.response().valueOpt().ifPresent(result::add);
    }
    return String.join(lineSeparator(), result);
  }
}
