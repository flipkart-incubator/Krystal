package com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello;

import static com.flipkart.krystal.vajram.inputs.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHelloRequest.friendIds_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHelloRequest.hellos_n;
import static com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHelloRequest.userIds_n;

import com.flipkart.krystal.vajram.ComputeVajram;
import com.flipkart.krystal.vajram.DependencyResponse;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramLogic;
import com.flipkart.krystal.vajram.inputs.MultiExecute;
import com.flipkart.krystal.vajram.inputs.Using;
import com.flipkart.krystal.vajram.inputs.resolution.Resolve;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHelloInputUtil.MutualFriendsHelloInputs;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@VajramDef("MutualFriendsHello")
public abstract class MutualFriendsHello extends ComputeVajram<String> {

  @VajramLogic
  public static String sayHelloToMutualFriends(
      MutualFriendsHelloInputs mutualFriendsHelloAllInputs) {
    List<String> result = new ArrayList<>();
    for (String userId : mutualFriendsHelloAllInputs.friendIds()) {
      mutualFriendsHelloAllInputs
          .hellos()
          .get(HelloFriendsV2Request.builder().userId(userId).build())
          .value()
          .ifPresent(result::add);
    }
    return String.join("\n", result);
  }

  @Resolve(depName = friendIds_n, depInputs = FriendsServiceRequest.userId_n)
  public static MultiExecute<String> userIdForFriendService(@Using(userIds_n) Set<String> userIds) {
    return executeFanoutWith(userIds);
  }

  @Resolve(depName = hellos_n, depInputs = HelloFriendsV2Request.userId_n)
  public static MultiExecute<String> userIDForHelloService(
      @Using(friendIds_n) DependencyResponse<FriendsServiceRequest, Set<String>> friendIdMap) {

    Set<String> friendIds =
        friendIdMap.values().asList().stream()
            .flatMap(val -> val.value().stream())
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    return executeFanoutWith(friendIds);
  }
}
